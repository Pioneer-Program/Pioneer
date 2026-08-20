package cute.ame.pioneer.Core.Observer;

import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public record ObserverState
(
    @Nullable PlanetDefinition body,
    Origin origin,
    int face,
    double u,
    double v,
    double altitudeKm,
    double bodyKmX,
    double bodyKmY,
    double bodyKmZ
)
{
    public enum Origin { SURFACE_BLOCKS, SUBLEVEL_POSE, ORBITAL_STATE, DEEP_SPACE }

    public static final ObserverState DEEP_SPACE = new ObserverState(null, Origin.DEEP_SPACE, -1, 0, 0, 0, 0, 0, 0);

    public boolean hasBody()
    {
        return body != null;
    }

    public boolean onFace()
    {
        return body != null && face >= 0;
    }

    public Vector3d bodyKm(Vector3d dest)
    {
        return dest.set(bodyKmX, bodyKmY, bodyKmZ);
    }

    public double radiusKm()
    {
        return Math.sqrt(bodyKmX * bodyKmX + bodyKmY * bodyKmY + bodyKmZ * bodyKmZ);
    }

    public double altitudeBlocks()
    {
        return onFace() ? altitudeKm * 1000.0 / Math.max(body.verticalScale(), 1.0e-6) : 0.0;
    }

    public double latDeg()
    {
        double r = radiusKm();
        return r < 1.0e-9 ? 0.0 : Math.toDegrees(Math.asin(Math.clamp(bodyKmY / r, -1.0, 1.0)));
    }

    public double lonDeg()
    {
        return CubeSurface.wrapDegrees(Math.toDegrees(Math.atan2(bodyKmX, bodyKmZ)));
    }

    public double blockX()
    {
        requireFace("blockX");
        return PlanetCube.faceOriginX(body, face) + u * PlanetCube.halfSide(body);
    }

    public double blockZ()
    {
        requireFace("blockZ");
        return v * PlanetCube.halfSide(body);
    }

    public boolean outOfFaceBounds()
    {
        return onFace() && (Math.abs(u) > 1.0 || Math.abs(v) > 1.0);
    }

    public static ObserverState fromSurfaceBlocks(PlanetDefinition body, double blockX, double blockY, double blockZ, Origin origin)
    {
        int face = PlanetCube.faceOf(body, blockX);
        double u = PlanetCube.toU(body, face, blockX);
        double v = PlanetCube.toV(body, blockZ);
        return ofFace(body, face, u, v, PlanetCube.altitudeKm(body, blockY), origin);
    }

    public static ObserverState ofFace(PlanetDefinition body, int face, double u, double v, double altKm, Origin origin)
    {
        Vector3d p = PlanetCube.facePointKm(body, face, u, v, altKm, new Vector3d());
        return new ObserverState(body, origin, face, u, v, altKm, p.x, p.y, p.z);
    }

    public static ObserverState fromBodyKm(PlanetDefinition body, double x, double y, double z, Origin origin)
    {
        double[] uv = new double[2];
        int face = CubeSurface.fromDirection(new Vector3d(x, y, z), uv);

        double[] w = CubeSurface.normal(face);
        double alt = (x * w[0] + y * w[1] + z * w[2]) - PlanetCube.halfExtentKm(body);
        return new ObserverState(body, origin, face, uv[0], uv[1], alt, x, y, z);
    }

    private void requireFace(String what)
    {
        if (!onFace())
            throw new IllegalStateException(what + "() called out of face (origin=" + origin + ", face=" + face + ")");
    }
}