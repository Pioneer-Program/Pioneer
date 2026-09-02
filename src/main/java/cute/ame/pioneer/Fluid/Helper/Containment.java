package cute.ame.pioneer.Fluid.Helper;

import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Physics.ComponentPartition;
import cute.ame.pioneer.Fluid.Physics.FluidPhase;
import net.minecraft.server.level.ServerLevel;

public final class Containment
{
    public static double of(ServerLevel level, FluidNodeStore store, FluidGraph graph, SpeciesTable table, int nodeId)
    {
        if (store.hasFlag(nodeId, FluidNodeStore.FLAG_OPEN))
        {
            AmbientState ambient = AmbientResolver.of(level.dimension());
            return Math.max(ambient.pressureP(), 0.0);
        }

        double network = lowestInNetwork(store, graph, nodeId);
        if (!Double.isNaN(network)) return network;

        return FluidPhase.selfContainment(store, nodeId, table);
    }

    private static double lowestInNetwork(FluidNodeStore store, FluidGraph graph, int nodeId)
    {
        ComponentPartition.Result partition = graph.partition();

        int component = partition.componentOf(nodeId);
        if (component < 0) return Double.NaN;

        int from = partition.nodeOffsets()[component];
        int to = partition.nodeOffsets()[component + 1];

        if (to - from <= 1) return Double.NaN;

        double lowest = Double.MAX_VALUE;
        for (int i = from; i < to; i++)
        {
            int other = partition.nodeOrder()[i];
            if (other == nodeId || !store.alive(other)) continue;

            double pressure = store.pressure(other);
            if (pressure < lowest) lowest = pressure;
        }

        return lowest == Double.MAX_VALUE ? Double.NaN : lowest;
    }
}
