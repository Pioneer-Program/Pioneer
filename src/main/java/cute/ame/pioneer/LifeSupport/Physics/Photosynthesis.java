package cute.ame.pioneer.LifeSupport.Physics;

public final class Photosynthesis
{
    public static final int FULL_LIGHT = 15;
    private static final float SETTLE = 1.0e-3f;

    public static float light(int brightness)
    {
        if (brightness <= 0) return 0.0f;

        return Math.min(brightness, FULL_LIGHT) / (float) FULL_LIGHT;
    }

    public static float capacity(int plants, float light, float molPerTick, int ticks)
    {
        if (plants <= 0 || light <= 0.0f || molPerTick <= 0.0f || ticks <= 0) return 0.0f;

        return plants * light * molPerTick * ticks;
    }

    public static float floorMoles(float moles, double pressure, float floorPressure)
    {
        if (floorPressure <= 0.0f || pressure <= 0.0 || moles <= 0.0f) return 0.0f;

        return (float) (floorPressure * moles / pressure);
    }

    public static float convertible(float co2, float floor, float capacity)
    {
        if (capacity <= 0.0f) return 0.0f;

        float headroom = co2 - floor;
        if (headroom <= floor * SETTLE) return 0.0f;

        return Math.min(capacity, headroom);
    }
}
