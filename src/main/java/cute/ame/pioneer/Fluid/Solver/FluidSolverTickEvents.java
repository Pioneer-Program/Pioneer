package cute.ame.pioneer.Fluid.Solver;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.FluidSpecies;
import cute.ame.pioneer.Fluid.Graph.ComponentPartition;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FluidSolverTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        FluidGraph graph = data.graph();

        graph.rebuildIfDirty(level, store);

        ComponentPartition.Result partition = graph.partition();
        if (partition.count() == 0) return;

        float[] molarHeat = FluidSpecies.active().molarHeatRaw();
        int sweeps = Config.FLUID_SWEEPS_PER_TICK.get();

        int[] edgeOrder = partition.edgeOrder();
        int[] edgeOffsets = partition.edgeOffsets();
        int[] edgeA = graph.edgeARaw();
        int[] edgeB = graph.edgeBRaw();
        float[] conductance = graph.conductanceRaw();

        boolean moved = false;

        for (int c = 0; c < partition.count(); c++)
        {
            int from = edgeOffsets[c];
            int to = edgeOffsets[c + 1];
            if (from == to) continue;

            double largest = 0.0;
            for (int pass = 0; pass < sweeps; pass++)
            {
                largest = FluidSolver.sweep(store, edgeOrder, from, to, edgeA, edgeB, conductance, molarHeat);
            }

            graph.setGap(c, largest);
            if (largest >= FluidSolver.EPSILON) moved = true;
        }

        if (moved) data.setDirty();
    }
}
