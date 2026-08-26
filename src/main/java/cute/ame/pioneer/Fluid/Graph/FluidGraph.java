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
    private static final Direction[] FORWARD =
    {
            Direction.EAST,
            Direction.UP,
            Direction.SOUTH
    };

    public static final double SETTLED = 1.0;

    private final LongOpenHashSet vessels = new LongOpenHashSet();

    private boolean dirty = true;

    private ComponentPartition.Result partition = ComponentPartition.Result.EMPTY;
    private int[] edgeA = new int[0];
    private int[] edgeB = new int[0];
    private float[] edgeConductance = new float[0];
    private int edgeCount;

    private double[] activity = new double[0];

    private int[] calm = new int[0];

    private boolean[] asleep = new boolean[0];
    private int awake;

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

    public int[] edgeARaw()
    {
        return edgeA;
    }

    public int[] edgeBRaw()
    {
        return edgeB;
    }

    public float[] conductanceRaw()
    {
        return edgeConductance;
    }

    public int awakeCount()
    {
        return awake;
    }

    public boolean isAsleep(int component)
    {
        return component >= 0 && component < asleep.length && asleep[component];
    }

    public double activity(int component)
    {
        return (component >= 0 && component < activity.length) ? activity[component] : 0.0;
    }

    public boolean settle(int component, double value, int patience)
    {
        if (component < 0 || component >= activity.length) return false;

        activity[component] = value;

        if (value >= SETTLED)
        {
            calm[component] = 0;
            return false;
        }

        if (++calm[component] < patience) return false;

        if (!asleep[component])
        {
            asleep[component] = true;
            awake--;
        }
        return true;
    }

    public void wakeNode(int nodeId)
    {
        wake(partition.componentOf(nodeId));
    }

    public void wake(int component)
    {
        if (component < 0 || component >= asleep.length) return;

        calm[component] = 0;
        if (!asleep[component]) return;

        asleep[component] = false;
        awake++;
    }

    public void wakeAll()
    {
        for (int c = 0; c < asleep.length; c++) wake(c);
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

            int node = nodeAt(level, pos, store);
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

                int other = nodeAt(level, cursor, store);
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
            if (b >= seen.length || seen[b]) continue;

            seen[b] = true;
            if (nodeCount == nodes.length) nodes = Arrays.copyOf(nodes, nodeCount * 2);
            nodes[nodeCount++] = b;
        }

        partition = ComponentPartition.of(nodes, nodeCount, edgeA, edgeB, edgeCount, maxNodeId);

        int count = partition.count();
        activity = new double[count];
        calm = new int[count];
        asleep = new boolean[count];
        awake = count;
        Arrays.fill(activity, Double.MAX_VALUE);
    }

    private void growEdges()
    {
        int next = Math.max(edgeA.length * 2, 16);
        edgeA = Arrays.copyOf(edgeA, next);
        edgeB = Arrays.copyOf(edgeB, next);
        edgeConductance = Arrays.copyOf(edgeConductance, next);
    }

    private static int nodeAt(ServerLevel level, BlockPos pos, FluidNodeStore store)
    {
        if (!(level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel)) return FluidNodeStore.INVALID;

        return store.resolve(vessel.getNodeHandle());
    }

    private static float conductanceAt(ServerLevel level, BlockPos pos)
    {
        return level.getBlockState(pos).getBlock() instanceof FluidVesselBlock vessel ? vessel.getConductance() : 0.0f;
    }
}
