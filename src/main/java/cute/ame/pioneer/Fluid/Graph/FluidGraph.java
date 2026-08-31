package cute.ame.pioneer.Fluid.Graph;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.Vessel.FluidVesselBlock;
import cute.ame.pioneer.Fluid.Vessel.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Vessel.PumpBlock;
import cute.ame.pioneer.Fluid.Vessel.ValveBlock;
import cute.ame.pioneer.Fluid.Vessel.VentBlock;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

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
    public static final float UNDIRECTED = -1.0f;
    private final LongOpenHashSet vessels = new LongOpenHashSet();

    private boolean dirty = true;

    private ComponentPartition.Result partition = ComponentPartition.Result.EMPTY;
    private int[] edgeA = new int[0];
    private int[] edgeB = new int[0];
    private float[] edgeConductance = new float[0];
    private float[] edgeBoost = new float[0];
    private int edgeCount;

    private long[] vesselOrder = new long[0];
    private int[] vesselStart = new int[1];
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

    public long[] vesselOrderRaw()
    {
        return vesselOrder;
    }

    public float[] boostRaw()
    {
        return edgeBoost;
    }

    public int vesselStart(int nodeId)
    {
        return (nodeId >= 0 && nodeId + 1 < vesselStart.length) ? vesselStart[nodeId] : 0;
    }

    public int vesselEnd(int nodeId)
    {
        return (nodeId >= 0 && nodeId + 1 < vesselStart.length) ? vesselStart[nodeId + 1] : 0;
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
        if (edgeA.length < capacity * 3) growEdges(capacity * 3);

        long[] scratchPos = new long[capacity];
        int[] scratchNode = new int[capacity];
        int vesselCount = 0;

        boolean[] seen = new boolean[store.getHighWater() + 1];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        float pumpBoost = Config.PUMP_BOOST_P.get().floatValue();

        for (long packed : vessels)
        {
            BlockPos pos = BlockPos.of(packed);
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FluidVesselBlock vessel)) continue;

            int node = nodeAt(level, pos, store);
            if (node == FluidNodeStore.INVALID) continue;

            if (vesselCount == scratchPos.length)
            {
                scratchPos = Arrays.copyOf(scratchPos, vesselCount * 2);
                scratchNode = Arrays.copyOf(scratchNode, vesselCount * 2);
            }
            scratchPos[vesselCount] = packed;
            scratchNode[vesselCount] = node;
            vesselCount++;

            if (!seen[node])
            {
                seen[node] = true;
                if (nodeCount == nodes.length) nodes = Arrays.copyOf(nodes, nodeCount * 2);
                nodes[nodeCount++] = node;
                if (node > maxNodeId) maxNodeId = node;
            }

            float here = conductanceOf(vessel, state);
            if (here <= 0.0f) continue;

            if (vessel instanceof VentBlock)
            {
                int room = VentBlock.roomAt(level, pos, state);
                if (room != FluidNodeStore.INVALID)
                {
                    addEdge(node, room, here, UNDIRECTED);
                    if (room > maxNodeId) maxNodeId = room;

                    if (room < seen.length && !seen[room])
                    {
                        seen[room] = true;
                        if (nodeCount == nodes.length) nodes = Arrays.copyOf(nodes, nodeCount * 2);
                        nodes[nodeCount++] = room;
                    }
                }
            }

            if (vessel instanceof PumpBlock)
            {
                maxNodeId = Math.max(maxNodeId, addPumpEdges(level, store, pos, state, node, here, pumpBoost));
                continue;
            }

            for (Direction direction : FORWARD)
            {
                cursor.setWithOffset(pos, direction);
                if (!vessels.contains(cursor.asLong())) continue;

                BlockState neighbourState = level.getBlockState(cursor);
                if (neighbourState.getBlock() instanceof PumpBlock) continue;
                if (!(neighbourState.getBlock() instanceof FluidVesselBlock neighbourVessel)) continue;

                int other = nodeAt(level, cursor, store);
                if (other == FluidNodeStore.INVALID || other == node) continue;

                float there = conductanceOf(neighbourVessel, neighbourState);
                if (there <= 0.0f) continue;

                addEdge(node, other, Math.min(here, there), UNDIRECTED);
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

        buildVesselIndex(scratchPos, scratchNode, vesselCount, maxNodeId);
        partition = ComponentPartition.of(nodes, nodeCount, edgeA, edgeB, edgeCount, maxNodeId);

        int count = partition.count();
        activity = new double[count];
        calm = new int[count];
        asleep = new boolean[count];
        awake = count;
        Arrays.fill(activity, Double.MAX_VALUE);
    }

    private int addPumpEdges(ServerLevel level, FluidNodeStore store, BlockPos pos, BlockState state, int node, float conductance, float boost)
    {
        Direction facing = state.getValue(PumpBlock.FACING);
        int max = node;

        BlockPos front = pos.relative(facing);
        int outlet = neighbourNode(level, store, front);
        if (outlet != FluidNodeStore.INVALID && outlet != node)
        {
            addEdge(node, outlet, conductance, boost);
            max = Math.max(max, outlet);
        }

        BlockPos back = pos.relative(facing.getOpposite());
        int inlet = neighbourNode(level, store, back);
        if (inlet != FluidNodeStore.INVALID && inlet != node)
        {
            addEdge(inlet, node, conductance, 0.0f);
            max = Math.max(max, inlet);
        }

        return max;
    }

    private int neighbourNode(ServerLevel level, FluidNodeStore store, BlockPos pos)
    {
        if (!vessels.contains(pos.asLong())) return FluidNodeStore.INVALID;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidVesselBlock vessel)) return FluidNodeStore.INVALID;
        if (conductanceOf(vessel, state) <= 0.0f) return FluidNodeStore.INVALID;

        return nodeAt(level, pos, store);
    }

    private void addEdge(int a, int b, float conductance, float boost)
    {
        if (edgeCount == edgeA.length) growEdges(Math.max(edgeA.length * 2, 16));

        edgeA[edgeCount] = a;
        edgeB[edgeCount] = b;
        edgeConductance[edgeCount] = conductance;
        edgeBoost[edgeCount] = boost;
        edgeCount++;
    }

    private void buildVesselIndex(long[] positions, int[] owners, int count, int maxNodeId)
    {
        vesselStart = new int[maxNodeId + 2];
        for (int i = 0; i < count; i++) vesselStart[owners[i] + 1]++;
        for (int n = 0; n < maxNodeId + 1; n++) vesselStart[n + 1] += vesselStart[n];

        vesselOrder = new long[count];
        int[] cursor = Arrays.copyOf(vesselStart, maxNodeId + 1);
        for (int i = 0; i < count; i++) vesselOrder[cursor[owners[i]]++] = positions[i];
    }

    private void growEdges(int next)
    {
        edgeA = Arrays.copyOf(edgeA, next);
        edgeB = Arrays.copyOf(edgeB, next);
        edgeConductance = Arrays.copyOf(edgeConductance, next);
        edgeBoost = Arrays.copyOf(edgeBoost, next);
    }

    private static int nodeAt(ServerLevel level, BlockPos pos, FluidNodeStore store)
    {
        if (!(level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel)) return FluidNodeStore.INVALID;

        return store.resolve(vessel.getNodeHandle());
    }

    private static float conductanceOf(FluidVesselBlock vessel, BlockState state)
    {
        if (vessel instanceof ValveBlock && !ValveBlock.isOpen(state)) return 0.0f;

        return vessel.getConductance();
    }
}
