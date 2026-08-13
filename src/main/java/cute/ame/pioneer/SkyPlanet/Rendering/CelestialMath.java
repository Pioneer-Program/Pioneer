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
        return new Quaternionf()
                .rotationZ((float) Math.toRadians(axialTiltDegrees))
                .rotateY(spinAngle);
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

    public static Quaternionf localHorizonRotation(float axialTiltDegrees, float solarDaySpeed, double orbitPeriodDays, double latitudeDeg, double longitudeDeg, long tick, double partialTick)
    {
        double spin = -siderealPhaseRadians(solarDaySpeed, orbitPeriodDays, tick, partialTick) + Math.toRadians(longitudeDeg);
        double phi = Math.toRadians(latitudeDeg);
        double cp = Math.cos(phi), sp = Math.sin(phi);
        double cs = Math.cos(spin), ss = Math.sin(spin);

        Vector3f up = new Vector3f((float) (cp * ss), (float) sp, (float) (cp * cs));
        Vector3f north = new Vector3f((float) (-sp * ss), (float) cp, (float) (-sp * cs));

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
