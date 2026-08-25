package cute.ame.pioneer.SkyPlanet.Physics;

import cute.ame.pioneer.SkyPlanet.Data.AtmosphereDefinition;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;

public final class SkyBrightness
{
    private static final double SIN_ASTRONOMICAL_TWILIGHT = Math.sin(Math.toRadians(-18.0));
    private static final double SIN_SUNRISE = Math.sin(Math.toRadians(0.0));

    public static float starVisibility(double sinSunAltitude, PlanetDefinition planet, double gravityMs2)
    {
        if (planet == null || planet.atmosphere().isEmpty()) return 1.0f;

        AtmosphereDefinition atmo = planet.atmosphere().get();

        double opacity = atmo.opacity(gravityMs2);
        double daylight = smoothstep(SIN_ASTRONOMICAL_TWILIGHT, SIN_SUNRISE, sinSunAltitude);

        return (float) Math.clamp(1.0 - daylight * opacity, 0.0, 1.0);
    }

    public static float starVisibility(double sinSunAltitude, PlanetDefinition planet)
    {
        if (planet == null) return 1.0f;
        return starVisibility(sinSunAltitude, planet, PlanetaryPhysics.surfaceGravityMs2(planet.massEarth(), planet.radiusKm()));
    }

    private static double smoothstep(double edge0, double edge1, double x)
    {
        double t = Math.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}