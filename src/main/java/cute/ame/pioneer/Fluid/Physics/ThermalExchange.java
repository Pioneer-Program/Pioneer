package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.FluidNodeStore;

public final class ThermalExchange
{
    public static final float EPSILON = 0.01f;
    
    private static final float FLOOR_K = 0.1f;

    public static double apply(FluidNodeStore store, int nodeId, float blockKelvin, double blockCapacity, float[] molarHeat, float rate)
    {
        if (store.moles(nodeId) <= 0.0f) return 0.0;

        float fluid = store.temperature(nodeId);
        float gap = blockKelvin - fluid;
        if (Math.abs(gap) < EPSILON) return 0.0;

        double step = rate <= 0.0f ? 0.0 : Math.min(rate, 1.0);
        if (step <= 0.0) return 0.0;

        double fluidCapacity = FluidHeat.capacity(store, nodeId, molarHeat);
        if (fluidCapacity <= 0.0) return 0.0;

        boolean bounded = Double.isFinite(blockCapacity) && blockCapacity > 0.0;
        double reduced = bounded ? fluidCapacity * blockCapacity / (fluidCapacity + blockCapacity) : fluidCapacity;

        double joules = step * reduced * gap;

        float next = fluid + (float) (joules / fluidCapacity);
        if (next < FLOOR_K) next = FLOOR_K;

        store.setTemperature(nodeId, next);

        return (next - fluid) * fluidCapacity;
    }
}
