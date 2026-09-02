package cute.ame.pioneer.Fluid.Event;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Physics.ThermalExchange;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ThermalCouplingTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        int period = Math.max(Config.FLUID_COUPLING_PERIOD.get(), 1);
        if (level.getGameTime() % period != 0) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        FluidGraph graph = data.graph();

        float rate = (float) Math.min(Config.FLUID_BLOCK_COUPLING.get() * period, 1.0);
        long[] vessels = graph.vesselOrderRaw();
        int high = store.getHighWater();

        for (int nodeId = 0; nodeId < high; nodeId++)
        {
            if (!store.alive(nodeId)) continue;

            int from = graph.vesselStart(nodeId);
            if (from >= graph.vesselEnd(nodeId)) continue;

            BlockPos pos = BlockPos.of(vessels[from]);
            float blockKelvin = BlockTemperature.of(level, pos);
            if (ThermalExchange.apply(store, nodeId, blockKelvin, rate)) data.touch(nodeId);
        }
    }
}
