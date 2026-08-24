package cute.ame.pioneer.Fluid.Solver;

import cute.ame.pioneer.Fluid.FluidNodeStore;

public final class FluidSolver
{
    public static final double EPSILON = 1.0e-7;

    public static double sweep(FluidNodeStore store, int[] edgeOrder, int from, int to, int[] edgeA, int[] edgeB, float[] conductance, float[] molarHeat)
    {
        double largest = 0.0;
        int stride = store.getStride();

        for (int i = from; i < to; i++)
        {
            int edge = edgeOrder[i];
            int a = edgeA[edge];
            int b = edgeB[edge];

            if (!store.alive(a) || !store.alive(b)) continue;

            double potentialA = FluidPotential.of(store, a);
            double potentialB = FluidPotential.of(store, b);
            double gap = potentialA - potentialB;

            double magnitude = Math.abs(gap);
            if (magnitude > largest) largest = magnitude;
            if (magnitude < EPSILON) continue;

            double slopes = FluidPotential.slope(store, a) + FluidPotential.slope(store, b);
            if (slopes <= 0.0) continue;

            double exact = gap / slopes;
            double moved = exact * conductance[edge];

            if (moved > 0.0) transfer(store, a, b, moved, stride, molarHeat);
            else if (moved < 0.0) transfer(store, b, a, -moved, stride, molarHeat);
        }

        return largest;
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

        double targetHeat = heatCapacity(store, target, stride, molarHeat) - movedHeat;
        if (targetHeat < 0.0) targetHeat = 0.0;

        double total = targetHeat + movedHeat;
        if (total <= 0.0) return;

        float mixed = (float) ((store.temperature(target) * targetHeat + store.temperature(source) * movedHeat) / total);
        store.setTemperature(target, mixed);
    }

    public static double heatCapacity(FluidNodeStore store, int nodeId, int stride, float[] molarHeat)
    {
        double sum = 0.0;
        int limit = Math.min(stride, molarHeat.length);

        for (int s = 0; s < limit; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol > 0.0f) sum += mol * molarHeat[s];
        }

        return sum;
    }
}
