package cute.ame.pioneer.Fluid.Event;

import cute.ame.pioneer.Fluid.Physics.BurstRule;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Physics.ComponentPartition;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FluidBurstTickEvents
{
    private static float[] limits = new float[64];
    private static long[] positions = new long[64];

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!Config.BURST_ENABLED.get()) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidGraph graph = data.graph();
        if (graph.awakeCount() == 0) return;

        FluidNodeStore store = data.store();
        ComponentPartition.Result partition = graph.partition();

        double screen = Config.BURST_SCREEN_PRESSURE.get();

        for (int c = 0; c < partition.count(); c++)
        {
            if (graph.isAsleep(c)) continue;

            for (int i = partition.nodeOffsets()[c]; i < partition.nodeOffsets()[c + 1]; i++)
            {
                int nodeId = partition.nodeOrder()[i];
                if (!store.alive(nodeId)) continue;

                double pressure = store.pressure(nodeId);
                if (pressure < screen) continue;

                burstWeakest(level, data, graph, store, nodeId, pressure);
            }
        }
    }

    private static void burstWeakest(ServerLevel level, FluidLevelData data, FluidGraph graph, FluidNodeStore store, int nodeId, double pressure)
    {
        int from = graph.vesselStart(nodeId);
        int to = graph.vesselEnd(nodeId);

        int count = to - from;
        if (count <= 0) return;

        if (limits.length < count)
        {
            limits = new float[count];
            positions = new long[count];
        }

        long[] vessels = graph.vesselOrderRaw();

        int found = 0;
        for (int i = from; i < to; i++)
        {
            long packed = vessels[i];
            if (!(level.getBlockEntity(BlockPos.of(packed)) instanceof FluidVesselBlockEntity vessel)) continue;

            positions[found] = packed;
            limits[found] = vessel.getBurstPressure();
            found++;
        }

        int weakest = BurstRule.weakest(limits, found, pressure);
        if (weakest == BurstRule.NONE) return;
        BlockPos pos = BlockPos.of(positions[weakest]);
        float limit = limits[weakest];

        store.clear(nodeId);
        data.touch(nodeId);

        level.destroyBlock(pos, false);
        float overload = (float) (pressure / Math.max(limit, 0.1f));
        float power = (float) Math.min(Config.BURST_EXPLOSION_POWER.get() * overload, Config.BURST_EXPLOSION_MAX_POWER.get());

        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);

        Pioneer.LOGGER.debug("[Pioneer] vessel burst at {} - {} P over a {} P limit, blast {}", pos, String.format("%.2f", pressure), String.format("%.2f", limit), String.format("%.2f", power));
    }
}
