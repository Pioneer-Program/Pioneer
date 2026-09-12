package cute.ame.pioneer.Fluid.Event;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Helper.FluidLevels;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Level.RoomLevelData;
import cute.ame.pioneer.Fluid.Physics.ComponentPartition;
import cute.ame.pioneer.Fluid.Physics.RoomScanner;
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
        if (store.getLiveCount() == 0) return;

        FluidGraph graph = data.graph();
        ComponentPartition.Result partition = graph.partition();

        float perBlock = (float) (Config.FLUID_BLOCK_COUPLING.get() * period);
        int maxSamples = Math.max(Config.FLUID_COUPLING_SAMPLES.get(), 1);

        long[] vessels = graph.vesselOrderRaw();
        int[] nodeOrder = partition.nodeOrder();
        int[] nodeOffsets = partition.nodeOffsets();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int c = 0, components = partition.count(); c < components; c++)
        {
            if (graph.isAsleep(c)) continue;

            for (int i = nodeOffsets[c], to = nodeOffsets[c + 1]; i < to; i++)
            {
                int nodeId = nodeOrder[i];
                if (!store.alive(nodeId)) continue;

                int from = graph.vesselStart(nodeId);
                int end = graph.vesselEnd(nodeId);
                if (from >= end) continue;

                couple(level, data, store, vessels, from, end, nodeId, perBlock, maxSamples, cursor);
            }
        }

        coupleRooms(level, data, store, graph, partition, perBlock, cursor);
    }

    private static void couple(ServerLevel level, FluidLevelData data, FluidNodeStore store, long[] vessels, int from, int end, int nodeId, float perBlock, int maxSamples, BlockPos.MutableBlockPos cursor)
    {
        int blocks = end - from;
        int samples = Math.min(blocks, maxSamples);
        int stride = blocks / samples;

        double sum = 0.0;
        int taken = 0;

        for (int k = 0; k < samples; k++)
        {
            long packed = vessels[from + k * stride];
            cursor.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));

            if (!FluidLevels.isLoaded(level, cursor)) continue;

            sum += BlockTemperature.of(level, cursor);
            taken++;
        }

        if (taken == 0) return;

        float blockKelvin = (float) (sum / taken);
        float rate = Math.min(1.0f, blocks * perBlock);

        if (ThermalExchange.apply(store, nodeId, blockKelvin, rate)) data.touch(nodeId);
    }

    private static void coupleRooms(ServerLevel level, FluidLevelData data, FluidNodeStore store, FluidGraph graph, ComponentPartition.Result partition, float perBlock, BlockPos.MutableBlockPos cursor)
    {
        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        if (rooms == null || rooms.roomCount() == 0) return;

        for (RoomLevelData.Room room : rooms.rooms())
        {
            int nodeId = room.nodeId();
            if (!store.alive(nodeId)) continue;

            if (graph.isAsleep(partition.componentOf(nodeId))) continue;

            long origin = room.origin();
            cursor.set(RoomScanner.unpackX(origin), RoomScanner.unpackY(origin), RoomScanner.unpackZ(origin));
            if (!FluidLevels.isLoaded(level, cursor)) continue;

            float blockKelvin = BlockTemperature.of(level, cursor);
            if (ThermalExchange.apply(store, nodeId, blockKelvin, perBlock)) data.touch(nodeId);
        }
    }
}
