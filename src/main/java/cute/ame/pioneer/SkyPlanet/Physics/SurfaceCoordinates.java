package cute.ame.pioneer.SkyPlanet.Physics;

import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;

public final class SurfaceCoordinates
{
    public static double blocksPerDegree(PlanetDefinition planet)
    {
        double circumferenceM = 2.0 * Math.PI * Math.max(planet.size(), 1e-3) * 1000.0;
        return circumferenceM / 360.0;
    }

    public static double effectiveBlocksPerDegree(PlanetDefinition planet)
    {
        double scale = Math.max(planet.surfaceScale(), 1e-6);
        return Math.max(blocksPerDegree(planet) / scale, 1e-6);
    }

    public static double latitudeDeg(PlanetDefinition planet, double blockX, double blockZ)
    {
        double along = planet.swapSurfaceAxes() ? blockX : blockZ;
        double deg = planet.originLatitude() - along / effectiveBlocksPerDegree(planet);
        return Math.clamp(deg, -90.0, 90.0);
    }

    public static double longitudeDeg(PlanetDefinition planet, double blockX, double blockZ)
    {
        double lat = latitudeDeg(planet, blockX, blockZ);
        double shrink = Math.max(Math.cos(Math.toRadians(lat)), 1e-3);
        double along = planet.swapSurfaceAxes() ? blockZ : blockX;

        double deg = planet.originLongitude() + along / (effectiveBlocksPerDegree(planet) * shrink);
        return wrapDegrees(deg);
    }

    public static double wrapDegrees(double deg)
    {
        double d = (deg + 180.0) % 360.0;
        if (d < 0.0) d += 360.0;
        return d - 180.0;
    }

    public static double blocksFromEquator(PlanetDefinition planet, double blockX, double blockZ)
    {
        return Math.abs(planet.swapSurfaceAxes() ? blockX : blockZ);
    }

    public static boolean insidePolarCircle(PlanetDefinition planet, double blockX, double blockZ)
    {
        return Math.abs(latitudeDeg(planet, blockX, blockZ)) + Math.abs(planet.axialTilt()) >= 90.0;
    }
}
