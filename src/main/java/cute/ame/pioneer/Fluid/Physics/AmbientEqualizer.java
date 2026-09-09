package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;

public final class AmbientEqualizer
{
    private static final float EMPTY_EPSILON = 1.0e-5f;

    public static boolean equalize(FluidNodeStore store, int id, AmbientState ambient, float rate, float epsilon)
    {
        int stride = store.getStride();
        if (stride == 0) return false;

        float step = rate <= 0.0f ? 0.0f : Math.min(rate, 1.0f);
        if (step <= 0.0f) return false;

        if (ambient.vacuum()) return drain(store, id, stride, step, epsilon);

        float temperature = store.temperature(id);
        if (temperature <= 0.0f) return false;

        double targetTotal = ambient.pressureP() * store.volume(id) / (FluidConstants.R * temperature);

        float[] amounts = store.getAmountsRaw();
        int base = id * stride;

        if (settled(amounts, base, stride, ambient, targetTotal, temperature, epsilon)) return false;

        for (int s = 0; s < stride; s++)
        {
            float target = (float) (targetTotal * ambient.fraction(s));
            float current = amounts[base + s];
            if (current == target) continue;

            float next = current + step * (target - current);
            amounts[base + s] = next > 0.0f ? next : 0.0f;
        }

        store.recomputeMoles(id);
        store.setTemperature(id, temperature + step * (ambient.temperatureK() - temperature));
        if (store.moles(id) < EMPTY_EPSILON) store.clear(id);

        return true;
    }

    private static boolean settled(float[] amounts, int base, int stride, AmbientState ambient, double targetTotal, float temperature, float epsilon)
    {
        if (Math.abs(ambient.temperatureK() - temperature) >= epsilon) return false;

        for (int s = 0; s < stride; s++)
        {
            float target = (float) (targetTotal * ambient.fraction(s));
            if (Math.abs(target - amounts[base + s]) >= epsilon) return false;
        }

        return true;
    }

    private static boolean drain(FluidNodeStore store, int id, int stride, float step, float epsilon)
    {
        float total = store.moles(id);
        if (total <= 0.0f) return false;

        float floor = Math.max(epsilon, EMPTY_EPSILON);
        if (total < floor)
        {
            store.clear(id);
            return true;
        }

        float keep = 1.0f - step;
        float[] amounts = store.getAmountsRaw();
        int base = id * stride;

        for (int s = 0; s < stride; s++)
        {
            float current = amounts[base + s];
            if (current > 0.0f) amounts[base + s] = current * keep;
        }

        store.recomputeMoles(id);
        if (store.moles(id) < EMPTY_EPSILON) store.clear(id);

        return true;
    }
}
