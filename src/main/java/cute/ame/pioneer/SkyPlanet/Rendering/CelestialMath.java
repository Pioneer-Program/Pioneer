package cute.ame.pioneer.SkyPlanet.Rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

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
