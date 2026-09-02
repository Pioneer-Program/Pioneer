package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.FluidNodeStore;

public final class ThermalExchange
{
    public static final float EPSILON = 0.01f;

    public static boolean apply(FluidNodeStore store, int nodeId, float blockKelvin, float rate)
    {
        if (store.moles(nodeId) <= 0.0f) return false;

        float fluid = store.temperature(nodeId);
        float gap = blockKelvin - fluid;
        if (Math.abs(gap) < EPSILON) return false;

        float step = rate <= 0.0f ? 0.0f : Math.min(rate, 1.0f);
        if (step <= 0.0f) return false;

        store.setTemperature(nodeId, fluid + step * gap);
        return true;
    }
}
