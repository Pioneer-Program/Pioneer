package cute.ame.pioneer.SkyPlanet.Physics;

public final class SurfaceCoordinates
{
    public static double wrapDegrees(double deg)
    {
        double d = (deg + 180.0) % 360.0;
        if (d < 0.0) d += 360.0;
        return d - 180.0;
    }
}