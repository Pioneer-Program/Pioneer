package cute.ame.pioneer.Fluid.Solver;

import cute.ame.pioneer.Fluid.FluidConstants;
import cute.ame.pioneer.Fluid.FluidNodeStore;

public final class FluidPotential
{
    public static final double LIQUID_LITRES_PER_MOL = 0.02;
    public static final double LIQUID_FULL_POTENTIAL = 10.0;

    public static double of(FluidNodeStore store, int nodeId)
    {
        if (store.hasFlag(nodeId, FluidNodeStore.FLAG_LIQUID)) return liquid(store, nodeId);

        return store.pressure(nodeId);
    }

    private static double liquid(FluidNodeStore store, int nodeId)
    {
        float volume = store.volume(nodeId);
        if (volume <= 0.0f) return 0.0;

        double fill = store.moles(nodeId) * LIQUID_LITRES_PER_MOL / volume;
        return fill * LIQUID_FULL_POTENTIAL;
    }

    public static double slope(FluidNodeStore store, int nodeId)
    {
        float volume = store.volume(nodeId);
        if (volume <= 0.0f) return 0.0;

        if (store.hasFlag(nodeId, FluidNodeStore.FLAG_LIQUID)) return LIQUID_LITRES_PER_MOL * LIQUID_FULL_POTENTIAL / volume;

        return FluidConstants.R * store.temperature(nodeId) / volume;
    }
}
