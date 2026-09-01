package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;

public final class AmbientEqualizer
{
    private static final float EMPTY_EPSILON = 1.0e-5f;

    public static void equalize(FluidNodeStore store, int id, AmbientState ambient, float rate)
    {
        int stride = store.getStride();
        if (stride == 0) return;

        float step = rate <= 0.0f ? 0.0f : Math.min(rate, 1.0f);
        if (step <= 0.0f) return;

        if (ambient.vacuum())
        {
            drain(store, id, stride, step);
            return;
        }

        float temperature = store.temperature(id);
        if (temperature <= 0.0f) return;

        double targetTotal = ambient.pressureP() * store.volume(id) / (FluidConstants.R * temperature);

        for (int s = 0; s < stride; s++)
        {
            float target = (float) (targetTotal * ambient.fraction(s));
            float current = store.amount(id, s);
            if (current == target) continue;

            store.setAmount(id, s, current + step * (target - current));
        }

        store.setTemperature(id, temperature + step * (ambient.temperatureK() - temperature));
        if (store.moles(id) < EMPTY_EPSILON) store.clear(id);

    }

    private static void drain(FluidNodeStore store, int id, int stride, float step)
    {
        if (store.moles(id) <= 0.0f) return;

        float keep = 1.0f - step;
        for (int s = 0; s < stride; s++)
        {
            float current = store.amount(id, s);
            if (current > 0.0f) store.setAmount(id, s, current * keep);
        }

        if (store.moles(id) < EMPTY_EPSILON) store.clear(id);
    }
}
