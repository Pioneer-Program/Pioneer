package cute.ame.pioneer.Core.Frame;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class FrameQuat
{
    public static final double IDENTITY_EPSILON = 1.0e-12;

    public static Quaterniond axisAngle(double ax, double ay, double az, double angle, Quaterniond dest)
    {
        double len = Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < IDENTITY_EPSILON || angle == 0.0) return dest.identity();

        double s = Math.sin(angle * 0.5) / len;
        return dest.set(ax * s, ay * s, az * s, Math.cos(angle * 0.5));
    }

    public static Quaterniond fromAxes(Vector3dc x, Vector3dc y, Vector3dc z, Quaterniond dest)
    {
        double trace = x.x() + y.y() + z.z();
        if (trace > 0.0)
        {
            double s = Math.sqrt(trace + 1.0) * 2.0;
            dest.set((y.z() - z.y()) / s, (z.x() - x.z()) / s, (x.y() - y.x()) / s, 0.25 * s);
        }
        else if (x.x() > y.y() && x.x() > z.z())
        {
            double s = Math.sqrt(1.0 + x.x() - y.y() - z.z()) * 2.0;
            dest.set(0.25 * s, (y.x() + x.y()) / s, (z.x() + x.z()) / s, (y.z() - z.y()) / s);
        }
        else if (y.y() > z.z())
        {
            double s = Math.sqrt(1.0 + y.y() - x.x() - z.z()) * 2.0;
            dest.set((y.x() + x.y()) / s, 0.25 * s, (z.y() + y.z()) / s, (z.x() - x.z()) / s);
        }
        else
        {
            double s = Math.sqrt(1.0 + z.z() - x.x() - y.y()) * 2.0;
            dest.set((z.x() + x.z()) / s, (z.y() + y.z()) / s, 0.25 * s, (x.y() - y.x()) / s);
        }

        return normalize(dest);
    }

    public static Quaterniond mul(Quaterniondc a, Quaterniondc b, Quaterniond dest)
    {
        double x = a.w() * b.x() + a.x() * b.w() + a.y() * b.z() - a.z() * b.y();
        double y = a.w() * b.y() - a.x() * b.z() + a.y() * b.w() + a.z() * b.x();
        double z = a.w() * b.z() + a.x() * b.y() - a.y() * b.x() + a.z() * b.w();
        double w = a.w() * b.w() - a.x() * b.x() - a.y() * b.y() - a.z() * b.z();
        return dest.set(x, y, z, w);
    }

    public static Quaterniond normalize(Quaterniond q)
    {
        double len = Math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w);
        if (len < IDENTITY_EPSILON) return q.identity();

        double inv = 1.0 / len;
        q.x *= inv;
        q.y *= inv;
        q.z *= inv;
        q.w *= inv;
        return q;
    }

    public static Vector3d rotate(Quaterniondc q, double vx, double vy, double vz, Vector3d dest)
    {
        double qx = q.x(), qy = q.y(), qz = q.z(), qw = q.w();
        double tx = 2.0 * (qy * vz - qz * vy);
        double ty = 2.0 * (qz * vx - qx * vz);
        double tz = 2.0 * (qx * vy - qy * vx);
        return dest.set(vx + qw * tx + (qy * tz - qz * ty), vy + qw * ty + (qz * tx - qx * tz), vz + qw * tz + (qx * ty - qy * tx));
    }

    public static boolean isIdentity(double qx, double qy, double qz)
    {
        return Math.abs(qx) < IDENTITY_EPSILON && Math.abs(qy) < IDENTITY_EPSILON && Math.abs(qz) < IDENTITY_EPSILON;
    }

    public static double angleDegrees(double qx, double qy, double qz, double qw)
    {
        double s = Math.sqrt(qx * qx + qy * qy + qz * qz);
        return Math.toDegrees(2.0 * Math.atan2(s, Math.abs(qw)));
    }
}
