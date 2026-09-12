package cute.ame.pioneer.Fluid.Helper;

import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;
import cute.ame.pioneer.Fluid.Physics.ContainmentCache;
import cute.ame.pioneer.Fluid.Physics.FluidPhase;

public final class Containment
{
    public static double of(FluidNodeStore store, SpeciesTable table, AmbientState ambient, int nodeId, int component, ContainmentCache cache)
    {
        if (store.hasFlag(nodeId, FluidNodeStore.FLAG_OPEN)) return Math.max(ambient.pressureP(), 0.0);

        double network = cache.lowestExcluding(component, nodeId);
        if (!Double.isNaN(network)) return network;

        return FluidPhase.selfContainment(store, nodeId, table);
    }
}
