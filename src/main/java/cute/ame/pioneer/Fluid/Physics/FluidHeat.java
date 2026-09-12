package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.FluidNodeStore;

public final class FluidHeat
{
    public static double capacity(FluidNodeStore store, int nodeId, float[] molarHeat)
    {
        int limit = Math.min(store.getStride(), molarHeat.length);
        double sum = 0.0;

        for (int s = 0; s < limit; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol > 0.0f) sum += mol * molarHeat[s];
        }

        return sum;
    }

    public static float addJoules(FluidNodeStore store, int nodeId, double joules, float[] molarHeat)
    {
        double heatCapacity = capacity(store, nodeId, molarHeat);
        if (heatCapacity <= 0.0) return 0.0f;

        float before = store.temperature(nodeId);
        float next = before + (float) (joules / heatCapacity);

        if (next < 0.1f) next = 0.1f;

        store.setTemperature(nodeId, next);
        return next - before;
    }
}
