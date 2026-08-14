package cute.ame.pioneer.SkyPlanet.Rendering;

import cute.ame.pioneer.SkyPlanet.Data.OrbitDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;

public final class PhysicalScale
{
    public static final float SOLAR_ANG_RAD_1AU = 4.6524e-3f;
    private static final double DAYS_PER_YEAR = 365.25;
    private static final float MAX_ANG_RAD = 0.30f;

    public static double semiMajorAxisAu(OrbitDefinition orbit, float massSolar)
    {
        double years = orbit.periodDays() / DAYS_PER_YEAR;
        if (!(years > 0.0)) return 1.0;
        return Math.cbrt(Math.max(massSolar, 1e-4) * years * years);
    }

    public static float sunAngularRadius(SunDefinition sun, OrbitDefinition orbit)
    {
        double au = semiMajorAxisAu(orbit, sun.massSolar());
        if (!(au > 1e-9)) return SOLAR_ANG_RAD_1AU;
        return (float) Math.min(SOLAR_ANG_RAD_1AU * sun.radiusSolar() / au, MAX_ANG_RAD);
    }
}
