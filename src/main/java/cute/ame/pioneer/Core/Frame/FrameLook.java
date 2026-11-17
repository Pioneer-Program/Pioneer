package cute.ame.pioneer.Core.Frame;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class FrameLook
{
    private static final double VERTICAL_EPSILON = 1.0e-9;

    public static Vector3d direction(double yawDegrees, double pitchDegrees, Vector3d dest)
    {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees), c = Math.cos(pitch);
        return dest.set(-Math.sin(yaw) * c, -Math.sin(pitch), Math.cos(yaw) * c);
    }

    public static float yaw(Vector3dc direction, float fallback)
    {
        return yaw(direction.x(), direction.z(), fallback);
    }

    public static float yaw(double x, double z, float fallback)
    {
        if (x * x + z * z < VERTICAL_EPSILON) return fallback;

        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    public static float pitch(Vector3dc direction)
    {
        return pitch(direction.x(), direction.y(), direction.z());
    }

    public static float pitch(double x, double y, double z)
    {
        return (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
    }
}
