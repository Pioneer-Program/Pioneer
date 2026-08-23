package cute.ame.pioneer.Fluid.Graph;

import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.Vessel.FluidVesselBlock;
import cute.ame.pioneer.Fluid.Vessel.FluidVesselBlockEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public final class FluidGraph
{
    private static final Direction[] FORWARD = { Direction.EAST, Direction.UP, Direction.SOUTH };

    private final LongOpenHashSet vessels = new LongOpenHashSet();

    private boolean dirty = true;

    private ComponentPartition.Result partition = ComponentPartition.Result.EMPTY;
    private int[] edgeA = new int[0];
    private int[] edgeB = new int[0];
    private float[] edgeConductance = new float[0];
    private int edgeCount;

    public void track(BlockPos pos)
    {
        if (vessels.add(pos.asLong())) dirty = true;
    }

    public void forget(BlockPos pos)
    {
        if (vessels.remove(pos.asLong())) dirty = true;
    }

    public void invalidate()
    {
        dirty = true;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public int vesselCount()
    {
        return vessels.size();
    }

    public ComponentPartition.Result partition()
    {
        return partition;
    }

    public int edgeCount()
    {
        return edgeCount;
    }

    public int edgeA(int edge)
    {
        return edgeA[edge];
    }

    public int edgeB(int edge)
    {
        return edgeB[edge];
    }

    public float conductance(int edge)
    {
        return edgeConductance[edge];
    }

    public void rebuildIfDirty(ServerLevel level, FluidNodeStore store)
    {
        if (!dirty) return;
        rebuild(level, store);
        dirty = false;
    }

    private void rebuild(ServerLevel level, FluidNodeStore store)
    {
        int capacity = Math.max(vessels.size(), 16);

        int[] nodes = new int[capacity];
        int nodeCount = 0;
        int maxNodeId = 0;

        edgeCount = 0;
        if (edgeA.length < capacity * 3)
        {
            edgeA = new int[capacity * 3];
            edgeB = new int[capacity * 3];
            edgeConductance = new float[capacity * 3];
        }

        boolean[] seen = new boolean[store.getHighWater() + 1];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (long packed : vessels)
        {
            BlockPos pos = BlockPos.of(packed);

            int node = nodeAt(level, pos);
            if (node == FluidNodeStore.INVALID) continue;

            if (!seen[node])
            {
                seen[node] = true;
                if (nodeCount == nodes.length) nodes = Arrays.copyOf(nodes, nodeCount * 2);
                nodes[nodeCount++] = node;
                if (node > maxNodeId) maxNodeId = node;
            }

            float here = conductanceAt(level, pos);
            if (here <= 0.0f) continue;

            for (Direction direction : FORWARD)
            {
                cursor.setWithOffset(pos, direction);
                if (!vessels.contains(cursor.asLong())) continue;

                int other = nodeAt(level, cursor);
                if (other == FluidNodeStore.INVALID || other == node) continue;

                float there = conductanceAt(level, cursor);
                if (there <= 0.0f) continue;

                if (edgeCount == edgeA.length) growEdges();

                edgeA[edgeCount] = node;
                edgeB[edgeCount] = other;
                edgeConductance[edgeCount] = Math.min(here, there);
                edgeCount++;

                if (other > maxNodeId) maxNodeId = other;
            }
        }

        for (int e = 0; e < edgeCount; e++)
        {
            int b = edgeB[e];
            if (b >= seen.length) continue;
            if (seen[b]) continue;

            seen[b] = true;
            if (nodeCount == nodes.length) nodes = Arrays.copyOf(nodes, nodeCount * 2);
            nodes[nodeCount++] = b;
        }

        partition = ComponentPartition.of(nodes, nodeCount, edgeA, edgeB, edgeCount, maxNodeId);
    }

    private void growEdges()
    {
        int next = Math.max(edgeA.length * 2, 16);
        edgeA = Arrays.copyOf(edgeA, next);
        edgeB = Arrays.copyOf(edgeB, next);
        edgeConductance = Arrays.copyOf(edgeConductance, next);
    }

    private static int nodeAt(ServerLevel level, BlockPos pos)
    {
        if (!(level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel)) return FluidNodeStore.INVALID;

        return cute.ame.pioneer.Fluid.Level.FluidLevelData.get(level).store().resolve(vessel.getNodeHandle());
    }

    private static float conductanceAt(ServerLevel level, BlockPos pos)
    {
        return level.getBlockState(pos).getBlock() instanceof FluidVesselBlock vessel ? vessel.getConductance() : 0.0f;
    }
}
