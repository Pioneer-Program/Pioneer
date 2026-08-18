package cute.ame.pioneer.SkyPlanet.Physics;

import cute.ame.pioneer.SkyPlanet.Data.OrbitDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;

public final class PhysicalScale
{
    public static final float SOLAR_ANG_RAD_1AU = 4.6524e-3f;
    private static final double DAYS_PER_YEAR = 365.25;
    private static final float MAX_ANG_RAD = 0.30f;
    public static final double SECONDS_PER_TICK = 3.6;
    public static final double KM_PER_SPACE_BLOCK = 1.0;

    public static double blocksPerTickToKmS(double blocksPerTick)
    {
        return blocksPerTick * KM_PER_SPACE_BLOCK / SECONDS_PER_TICK;
    }

    public static double kmSToBlocksPerTick(double kmPerSecond)
    {
        return kmPerSecond * SECONDS_PER_TICK / KM_PER_SPACE_BLOCK;
    }

    public static double spinRateRadPerTick(float axialRotationSpeedDays)
    {
        double dayTicks = Math.max(axialRotationSpeedDays, 1e-6) * 24000.0;
        return (2.0 * Math.PI) / dayTicks;
    }

    public static double entrainmentBlocksPerTick(float radiusKm, float axialRotationSpeedDays, double upY)
    {
        double axisDistance = Math.max(radiusKm, 1e-3) * Math.sqrt(Math.max(0.0, 1.0 - upY * upY));
        return spinRateRadPerTick(axialRotationSpeedDays) * axisDistance;
    }

    public static double semiMajorAxisAu(OrbitDefinition orbit, float massSolar)
    {
        double years = orbit.periodDays() / DAYS_PER_YEAR;
        if (!(years > 0.0)) return 1.0;
        return Math.cbrt(Math.max(massSolar, 1e-4) * years * years);
    }

    public static float sunAngularRadius(SunDefinition sun, double distanceAu)
    {
        if (!(distanceAu > 1e-9)) return SOLAR_ANG_RAD_1AU;
        return (float) Math.min(SOLAR_ANG_RAD_1AU * sun.radiusSolar() / distanceAu, MAX_ANG_RAD);
    }

    public static float sunAngularRadius(SunDefinition sun, OrbitDefinition orbit)
    {
        return sunAngularRadius(sun, semiMajorAxisAu(orbit, sun.massSolar()));
    }
}