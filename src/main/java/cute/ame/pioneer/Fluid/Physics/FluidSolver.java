package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.FluidNodeStore;

public final class FluidSolver
{
    public static final double EPSILON = 1.0e-7;
    public static final float UNDIRECTED = -1.0f;

    public static double sweep(FluidNodeStore store, int[] edgeOrder, int from, int to, int[] edgeA, int[] edgeB, float[] conductance, float[] boost, float[] molarHeat, float thermalConductance, double potentialEpsilon, double temperatureEpsilon)
    {
        double activity = 0.0;
        int stride = store.getStride();

        for (int i = from; i < to; i++)
        {
            int edge = edgeOrder[i];
            int a = edgeA[edge];
            int b = edgeB[edge];

            if (!store.alive(a) || !store.alive(b)) continue;

            double potentialA = FluidPotential.of(store, a);
            double potentialB = FluidPotential.of(store, b);

            float drive = boost[edge];
            boolean directed = drive >= 0.0f;
            double gap = directed ? (potentialA + drive) - potentialB : potentialA - potentialB;

            if (directed && gap <= 0.0)
            {
                double temperatureIdle = Math.abs(store.temperature(a) - store.temperature(b));
                if (temperatureIdle / temperatureEpsilon > activity) activity = temperatureIdle / temperatureEpsilon;
                if (thermalConductance > 0.0f && temperatureIdle > 0.0) conduct(store, a, b, stride, molarHeat, thermalConductance);
                continue;
            }

            double magnitude = Math.abs(gap);
            if (magnitude / potentialEpsilon > activity) activity = magnitude / potentialEpsilon;

            if (magnitude >= EPSILON)
            {
                double slopes = FluidPotential.slope(store, a) + FluidPotential.slope(store, b);
                if (slopes > 0.0)
                {
                    double moved = gap / slopes * conductance[edge];

                    if (moved > 0.0) transfer(store, a, b, moved, stride, molarHeat);
                    else if (moved < 0.0) transfer(store, b, a, -moved, stride, molarHeat);
                }
            }

            double temperatureGap = Math.abs(store.temperature(a) - store.temperature(b));
            if (temperatureGap / temperatureEpsilon > activity) activity = temperatureGap / temperatureEpsilon;

            if (thermalConductance > 0.0f && temperatureGap > 0.0) conduct(store, a, b, stride, molarHeat, thermalConductance);
        }

        return activity;
    }

    public static double sweep(FluidNodeStore store, int[] edgeOrder, int from, int to, int[] edgeA, int[] edgeB, float[] conductance, float[] molarHeat, float thermalConductance, double potentialEpsilon, double temperatureEpsilon)
    {
        float[] undirected = new float[edgeA.length];
        java.util.Arrays.fill(undirected, UNDIRECTED);

        return sweep(store, edgeOrder, from, to, edgeA, edgeB, conductance, undirected, molarHeat, thermalConductance, potentialEpsilon, temperatureEpsilon);
    }

    private static void transfer(FluidNodeStore store, int source, int target, double amount, int stride, float[] molarHeat)
    {
        float available = store.moles(source);
        if (available <= 0.0f) return;

        if (amount > available) amount = available;

        double scale = amount / available;

        double movedHeat = 0.0;
        for (int s = 0; s < stride; s++)
        {
            float mol = store.amount(source, s);
            if (mol <= 0.0f) continue;

            float moved = (float) (mol * scale);
            if (moved <= 0.0f) continue;

            store.add(source, s, -moved);
            store.add(target, s, moved);

            if (s < molarHeat.length) movedHeat += moved * molarHeat[s];
        }

        if (movedHeat <= 0.0) return;

        double targetHeat = FluidHeat.capacity(store, target, molarHeat) - movedHeat;
        if (targetHeat < 0.0) targetHeat = 0.0;

        double total = targetHeat + movedHeat;
        if (total <= 0.0) return;

        store.setTemperature(target, (float) ((store.temperature(target) * targetHeat + store.temperature(source) * movedHeat) / total));
    }

    private static void conduct(FluidNodeStore store, int a, int b, int stride, float[] molarHeat, float rate)
    {
        double capacityA = FluidHeat.capacity(store, a, molarHeat);
        double capacityB = FluidHeat.capacity(store, b, molarHeat);

        if (capacityA <= 0.0 || capacityB <= 0.0) return;

        float temperatureA = store.temperature(a);
        float temperatureB = store.temperature(b);

        double shared = (capacityA * temperatureA + capacityB * temperatureB) / (capacityA + capacityB);
        double step = Math.min(rate, 1.0f);

        store.setTemperature(a, (float) (temperatureA + step * (shared - temperatureA)));
        store.setTemperature(b, (float) (temperatureB + step * (shared - temperatureB)));
    }
}
