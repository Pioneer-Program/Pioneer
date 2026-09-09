package cute.ame.pioneer.Fluid.Event;

import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Helper.AmbientResolver;
import cute.ame.pioneer.Fluid.Helper.Containment;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Physics.ComponentPartition;
import cute.ame.pioneer.Fluid.Physics.ContainmentCache;
import cute.ame.pioneer.Fluid.Physics.FluidPhase;
import cute.ame.pioneer.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FluidPhaseTickEvents
{
    private static final ContainmentCache CONTAINMENT = new ContainmentCache();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        int high = store.getHighWater();
        if (high == 0) return;

        SpeciesTable table = FluidSpecies.active();
        FluidGraph graph = data.graph();
        ComponentPartition.Result partition = graph.partition();
        AmbientState ambient = AmbientResolver.of(level.dimension());

        CONTAINMENT.rebuild(store, graph, partition);

        for (int id = 0; id < high; id++)
        {
            if (!store.alive(id)) continue;

            int component = partition.componentOf(id);
            if (component >= 0 && graph.isAsleep(component)) continue;

            double containment = store.isLiquid(id) ? Containment.of(store, table, ambient, id, component, CONTAINMENT) : FluidPhase.DEFAULT_CONTAINMENT_P;
            if (FluidPhase.update(store, id, table, containment)) data.touch(id);
        }
    }
}
