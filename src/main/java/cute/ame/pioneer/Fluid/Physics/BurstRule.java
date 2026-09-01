package cute.ame.pioneer.Fluid.Physics;

public final class BurstRule
{
    public static final int NONE = -1;

    public static int weakest(float[] limits, int count, double pressure)
    {
        int weakest = NONE;
        float lowest = Float.MAX_VALUE;

        for (int i = 0; i < count; i++)
        {
            float limit = limits[i];
            if (limit <= 0.0f || pressure < limit) continue;

            if (limit < lowest)
            {
                lowest = limit;
                weakest = i;
            }
        }

        return weakest;
    }

    public static float jitter(float nominal, float amplitude, float roll)
    {
        float span = nominal * amplitude;
        return Math.max(nominal - span + 2.0f * span * roll, 0.1f);
    }
}
