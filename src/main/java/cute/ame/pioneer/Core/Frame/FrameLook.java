package cute.ame.pioneer.Core.Frame;

public final class FrameLook
{
    private static final double VERTICAL_EPSILON = 1.0e-9;

    public static double[] direction(double yawDegrees, double pitchDegrees, double[] out)
    {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees), c = Math.cos(pitch);
        out[0] = -Math.sin(yaw) * c;
        out[1] = -Math.sin(pitch);
        out[2] = Math.cos(yaw) * c;
        return out;
    }

    public static float yaw(double x, double z, float fallback)
    {
        if (x * x + z * z < VERTICAL_EPSILON) return fallback;

        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    public static float pitch(double x, double y, double z)
    {
        return (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
    }
}
