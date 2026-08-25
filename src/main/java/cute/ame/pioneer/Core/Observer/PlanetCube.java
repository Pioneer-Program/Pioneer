package cute.ame.pioneer.Core.Observer;

import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import org.joml.Vector3d;

public final class PlanetCube
{
    public static final double REFERENCE_LEVEL = 63.0;

    public static double halfSide(PlanetDefinition body)
    {
        return CubeSurface.halfSide(body.radiusKm(), body.surfaceScale());
    }

    public static double halfExtentKm(PlanetDefinition body)
    {
        return body.radiusKm();
    }

    public static double altitudeKm(PlanetDefinition body, double blockY)
    {
        return (blockY - REFERENCE_LEVEL) * body.verticalScale() * 1.0e-3;
    }

    public static double blockY(PlanetDefinition body, double altitudeKm)
    {
        return altitudeKm * 1000.0 / Math.max(body.verticalScale(), 1.0e-6) + REFERENCE_LEVEL;
    }

    public static double faceOriginX(PlanetDefinition body, int face)
    {
        return CubeSurface.faceOriginX(halfSide(body), face);
    }

    public static int faceOf(PlanetDefinition body, double blockX)
    {
        return CubeSurface.faceOf(halfSide(body), blockX);
    }

    public static double toU(PlanetDefinition body, int face, double blockX)
    {
        return CubeSurface.toU(halfSide(body), face, blockX);
    }

    public static double toV(PlanetDefinition body, double blockZ)
    {
        return CubeSurface.toV(halfSide(body), blockZ);
    }

    public static Vector3d facePointKm(PlanetDefinition body, int face, double u, double v, double altKm, Vector3d dest)
    {
        return CubeSurface.facePointKm(halfExtentKm(body), face, u, v, altKm, dest);
    }

    public static CubeSurface.Crossing cross(PlanetDefinition body, int face, double u, double v)
    {
        return CubeSurface.cross(halfSide(body), face, u, v);
    }

    public static double homeBlockX(PlanetDefinition body)
    {
        return faceOriginX(body, body.homeFace()) + body.homeU() * halfSide(body);
    }

    public static double homeBlockZ(PlanetDefinition body)
    {
        return body.homeV() * halfSide(body);
    }
}