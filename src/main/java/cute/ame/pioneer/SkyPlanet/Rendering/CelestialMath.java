package cute.ame.pioneer.SkyPlanet.Rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CelestialMath
{
    public static final long TICKS_PER_DAY = 24000L;

    public static void poseAxialRotation(PoseStack ps, float speed, long tick, float partialTick)
    {
        ps.mulPose(new Quaternionf().rotationY(axialPhaseRadians(speed, tick, partialTick)));
    }

    public static Quaternionf planetOrientation(float axialTiltDegrees, float axialRotationSpeedDays, long tick, float partialTick)
    {
        float spinAngle = axialPhaseRadians(axialRotationSpeedDays, tick, partialTick);
        return new Quaternionf().rotationZ((float) Math.toRadians(axialTiltDegrees)).rotateY(spinAngle);
    }

    public static float axialPhaseRadians(float axialRotationSpeedDays, long tick, float partialTick)
    {
        double phase = ((tick + partialTick) / (double) (axialRotationSpeedDays * TICKS_PER_DAY)) % 1.0;
        return (float) (phase * 2.0 * Math.PI);
    }

    public static void poseOrientation(PoseStack ps, float axialTiltDegrees, float axialRotationSpeedDays, long tick, float partialTick)
    {
        ps.mulPose(planetOrientation(axialTiltDegrees, axialRotationSpeedDays, tick, partialTick));
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    public static double siderealPhaseRadians(float solarDaySpeed, double orbitPeriodDays, long tick, double partialTick)
    {
        double t = tick + partialTick;
        double invSolar = (solarDaySpeed != 0.0f) ? 1.0 / (solarDaySpeed * TICKS_PER_DAY) : 0.0;
        double invOrbit = (orbitPeriodDays > 1e-9) ? 1.0 / (orbitPeriodDays * TICKS_PER_DAY) : 0.0;
        return Math.TAU * (t * (invSolar + invOrbit));
    }

    public static Quaternionf localHorizonRotation(float axialTiltDegrees, float solarDaySpeed, double latitudeDeg, double longitudeDeg, Vector3f worldSunDir, long tick, double partialTick)
    {
        Quaternionf tiltInv = new Quaternionf().rotationZ((float) Math.toRadians(-axialTiltDegrees));
        Vector3f sunEq = tiltInv.transform(new Vector3f(worldSunDir));

        double lambdaSun = Math.atan2(sunEq.x, sunEq.z);
        double dayTicks = Math.max(solarDaySpeed, 1e-6) * TICKS_PER_DAY;
        double frac = ((tick + partialTick) % dayTicks) / dayTicks;
        double phi = lambdaSun + Math.TAU * (frac - 0.25) + Math.toRadians(longitudeDeg);

        double lat = Math.toRadians(latitudeDeg);
        double cl = Math.cos(lat), sl = Math.sin(lat);
        double sp = Math.sin(phi), cp = Math.cos(phi);

        Vector3f up = new Vector3f((float) (cl * sp), (float) sl, (float) (cl * cp));
        Vector3f north = new Vector3f((float) (-sl * sp), (float) cl, (float) (-sl * cp));

        Quaternionf tilt = new Quaternionf().rotationZ((float) Math.toRadians(axialTiltDegrees));
        tilt.transform(up).normalize();
        tilt.transform(north).normalize();

        Vector3f east  = new Vector3f(north).cross(up).normalize();
        Vector3f south = new Vector3f(north).negate();
        Matrix3f basis = new Matrix3f(east, up, south);
        return basis.transpose().getNormalizedRotation(new Quaternionf());
    }

    public static final float[][][] CUBE_FACES =
    {
        {{-1, 1, 1},{1, 1, 1},{1, 1,-1},{-1, 1,-1}},
        {{-1,-1,-1},{1,-1,-1},{1,-1, 1},{-1,-1, 1}},
        {{ 1, 1, 1},{-1,1, 1},{-1,-1,1},{ 1,-1, 1}},
        {{ 1, 1,-1},{-1,1,-1},{-1,-1,-1},{1,-1,-1}},
        {{ 1, 1,-1},{1, 1, 1},{1,-1, 1},{1,-1,-1}},
        {{-1, 1, 1},{-1,1,-1},{-1,-1,-1},{-1,-1,1}},
    };
}
