package cute.ame.pioneer.Core.Observer;

import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LocalFrame
{
    private static PlanetDefinition cachedBody;
    private static int cachedFace = -1;
    private static long cachedTick = Long.MIN_VALUE;
    private static float cachedPartial = -1f;
    private static Quaternionf cached;

    public static Quaternionf horizon(PlanetDefinition body, int face, long tick, float partialTick)
    {
        if (body == cachedBody && face == cachedFace && tick == cachedTick && partialTick == cachedPartial) return cached;

        double[] w = CubeSurface.normal(face);
        double[] n = new double[3];
        CubeSurface.north(face, n);

        Quaternionf spin = CelestialMath.planetOrientation(body.axialTilt(), body.axialRotationSpeed(), tick, partialTick);

        Vector3f up = spin.transform(new Vector3f((float) w[0], (float) w[1], (float) w[2])).normalize();
        Vector3f north = spin.transform(new Vector3f((float) n[0], (float) n[1], (float) n[2])).normalize();
        Vector3f east = new Vector3f(north).cross(up).normalize();
        Vector3f south = new Vector3f(north).negate();

        cached = new Matrix3f(east, up, south).transpose().getNormalizedRotation(new Quaternionf());
        cachedBody = body; cachedFace = face; cachedTick = tick; cachedPartial = partialTick;
        return cached;
    }

    public static Quaternionf horizon(ObserverState obs, long tick, float partialTick)
    {
        return obs.onFace() && obs.hasBody() ? horizon(obs.body(), obs.face(), tick, partialTick) : new Quaternionf();
    }

    public static float sunAltitudeSin(PlanetDefinition body, int face, Vector3f worldSunDir, long tick, float partialTick)
    {
        return horizon(body, face, tick, partialTick).transform(new Vector3f(worldSunDir)).y;
    }

    public static void invalidate()
    {
        cachedTick = Long.MIN_VALUE;
        cachedBody = null; cachedFace = -1;
    }
}