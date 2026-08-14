package cute.ame.pioneer.SkyPlanet.Physics;

public final class PlanetaryPhysics
{
    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double EARTH_GRAVITY_MS2 = 9.80665;

    public static double surfaceGravityMs2(double massEarth, double radiusKm)
    {
        double r = Math.max(radiusKm, 1e-3) / EARTH_RADIUS_KM;
        return EARTH_GRAVITY_MS2 * Math.max(massEarth, 1e-6) / (r * r);
    }

    public static double surfaceGravityRelative(double massEarth, double radiusKm)
    {
        return surfaceGravityMs2(massEarth, radiusKm) / EARTH_GRAVITY_MS2;
    }

    public static double escapeVelocityKmS(double massEarth, double radiusKm)
    {
        double g = surfaceGravityMs2(massEarth, radiusKm);
        return Math.sqrt(2.0 * g * Math.max(radiusKm, 1e-3) * 1000.0) / 1000.0;
    }

    public static double equilibriumTemperatureK(double starTemperatureK, double starRadiusSolar, double distanceAu, double bondAlbedo)
    {
        double rStarAu = Math.max(starRadiusSolar, 1e-9) * 0.00465047;
        double d = Math.max(distanceAu, 1e-6);
        double a = Math.max(0.0, Math.min(0.99, bondAlbedo));
        return starTemperatureK * Math.sqrt(rStarAu / (2.0 * d)) * Math.pow(1.0 - a, 0.25);
    }

    public static double greenhouseLiftK(double surfacePressureBar, double greenhouseFraction)
    {
        double p = Math.max(surfacePressureBar, 0.0);
        return 500.0 * Math.clamp(greenhouseFraction, 0.0, 1.0) * (1.0 - Math.exp(-p / 20.0));
    }
}
