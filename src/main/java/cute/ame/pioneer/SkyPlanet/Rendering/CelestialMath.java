package cute.ame.pioneer.SkyPlanet.Rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

public final class CelestialMath
{
    public static final long TICKS_PER_DAY = 24000L;

    public static float axialPhaseRadians(float axialRotationSpeedDays, long tick, float partialTick)
    {
        double dayTicks = axialRotationSpeedDays * (double) TICKS_PER_DAY;
        if (!(Math.abs(dayTicks) > 1.0e-6)) return 0.0f;
        double phase = ((tick + partialTick) / dayTicks) % 1.0;
        return (float) (phase * 2.0 * Math.PI);
    }

    public static Quaternionf planetOrientation(float axialTiltDegrees, float axialRotationSpeedDays, long tick, float partialTick)
    {
        return new Quaternionf().rotationZ((float) Math.toRadians(axialTiltDegrees)).rotateY(axialPhaseRadians(axialRotationSpeedDays, tick, partialTick));
    }

    public static void poseAxialRotation(PoseStack ps, float speed, long tick, float partialTick)
    {
        ps.mulPose(new Quaternionf().rotationY(axialPhaseRadians(speed, tick, partialTick)));
    }
}