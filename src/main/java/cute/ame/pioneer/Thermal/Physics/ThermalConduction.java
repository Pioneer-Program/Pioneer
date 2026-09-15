package cute.ame.pioneer.Thermal.Physics;

public final class ThermalConduction
{
    public static final float SAFE_STEP = 1.0f / 6.0f;

    public static float alpha(float kEff, float ca, float cb, float dt, float maxStep)
    {
        float a = kEff * dt * (ca + cb) / (ca * cb);
        return Math.min(a, maxStep);
    }

    public static float delta(float kEff, float ca, float cb, float ta, float tb, float dt, float maxStep)
    {
        float diff = tb - ta;
        if (diff == 0.0f) return 0.0f;

        return alpha(kEff, ca, cb, dt, maxStep) * (cb / (ca + cb)) * diff;
    }

    public static float joules(float kEff, float ca, float cb, float ta, float tb, float dt, float maxStep)
    {
        return delta(kEff, ca, cb, ta, tb, dt, maxStep) * ca;
    }
}
