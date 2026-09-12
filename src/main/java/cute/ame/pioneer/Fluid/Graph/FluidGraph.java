package cute.ame.pioneer.Fluid.Graph;

import cute.ame.pioneer.Fluid.Physics.ComponentPartition;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Block.FluidVesselBlock;
import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Block.PumpBlock;
import cute.ame.pioneer.Fluid.Block.ValveBlock;
import cute.ame.pioneer.Fluid.Block.VentBlock;
import cute.ame.pioneer.Fluid.Physics.FluidSolver;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
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
    private final LongOpenHashSet vessels = new LongOpenHashSet();
    private final Long2IntOpenHashMap pumpOutlet = new Long2IntOpenHashMap();

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

    public FluidGraph()
    {
        pumpOutlet.defaultReturnValue(-1);
    }

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
        pumpOutlet.clear();
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
                    addEdge(node, room, here, FluidSolver.UNDIRECTED);
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
                maxNodeId = Math.max(maxNodeId, addPumpEdges(level, store, pos, state, node, here, pumpBoost * powerAt(level, pos)));
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

                addEdge(node, other, Math.min(here, there), FluidSolver.UNDIRECTED);
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

        int outletEdge = edgeCount;
        max = Math.max(max, linkPump(level, store, pos.relative(facing), node, conductance, boost, true));
        if (edgeCount > outletEdge) pumpOutlet.put(pos.asLong(), outletEdge);

        max = Math.max(max, linkPump(level, store, pos.relative(facing.getOpposite()), node, conductance, 0.0f, false));
        return max;
    }

    public boolean setPumpBoost(BlockPos pos, float boost)
    {
        if (dirty) return false;

        int edge = pumpOutlet.get(pos.asLong());
        if (edge < 0 || edge >= edgeCount)
        {
            invalidate();
            return false;
        }

        if (edgeBoost[edge] == boost) return false;

        edgeBoost[edge] = boost;
        wakeNode(edgeA[edge]);
        return true;
    }

    private static float powerAt(ServerLevel level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel ? vessel.getPower() : 1.0f;
    }

    private int linkPump(ServerLevel level, FluidNodeStore store, BlockPos side, int node, float conductance, float boost, boolean outward)
    {
        if (!vessels.contains(side.asLong())) return node;

        BlockState state = level.getBlockState(side);
        if (!(state.getBlock() instanceof FluidVesselBlock vessel)) return node;

        float there = conductanceOf(vessel, state);
        if (there <= 0.0f) return node;

        int other = nodeAt(level, side, store);
        if (other == FluidNodeStore.INVALID || other == node) return node;

        float shared = Math.min(conductance, there);
        if (outward) addEdge(node, other, shared, boost);
        else addEdge(other, node, shared, boost);

        return other;
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

        if (vessel instanceof PumpBlock && !PumpBlock.isActive(state)) return 0.0f;

        return vessel.getConductance();
    }
}
