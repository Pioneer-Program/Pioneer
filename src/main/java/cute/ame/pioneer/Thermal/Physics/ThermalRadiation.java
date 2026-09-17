package cute.ame.pioneer.Thermal.Physics;

public final class ThermalRadiation
{
    public static final float SIGMA = 5.670374419e-8f;

    public static float delta(float emissivity, float area, int faces, float capacity, float kelvin, float sink, float dt, float maxStep)
    {
        if (emissivity <= 0.0f || area <= 0.0f || faces <= 0) return 0.0f;

        float block = kelvin * kelvin;
        float background = sink * sink;
        float watts = SIGMA * emissivity * area * faces * (block * block - background * background);
        float change = -watts * dt / capacity;
        float limit = Math.abs(sink - kelvin) * maxStep;
        if (change > limit) return limit;
        return Math.max(change, -limit); //TODO: might fucked up radiation, need more test before merging
    }

    public static float watts(float emissivity, float area, int faces, float kelvin, float sink)
    {
        if (emissivity <= 0.0f || area <= 0.0f || faces <= 0) return 0.0f;

        float block = kelvin * kelvin;
        float background = sink * sink;

        return SIGMA * emissivity * area * faces * (block * block - background * background);
    }
}
