package cute.ame.pioneer.Core.Frame;

public final class FrameQuat
{
    public static final double IDENTITY_EPSILON = 1.0e-12;

    public static double[] identity(double[] q)
    {
        q[0] = 0.0;
        q[1] = 0.0;
        q[2] = 0.0;
        q[3] = 1.0;
        return q;
    }

    public static double[] axisAngle(double ax, double ay, double az, double angle, double[] out)
    {
        double len = Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < IDENTITY_EPSILON || angle == 0.0) return identity(out);

        double s = Math.sin(angle * 0.5) / len;
        out[0] = ax * s;
        out[1] = ay * s;
        out[2] = az * s;
        out[3] = Math.cos(angle * 0.5);
        return out;
    }

    public static double[] fromAxes(double[] x, double[] y, double[] z, double[] out)
    {
        double trace = x[0] + y[1] + z[2];
        if (trace > 0.0)
        {
            double s = Math.sqrt(trace + 1.0) * 2.0;
            out[0] = (y[2] - z[1]) / s;
            out[1] = (z[0] - x[2]) / s;
            out[2] = (x[1] - y[0]) / s;
            out[3] = 0.25 * s;
        }
        else if (x[0] > y[1] && x[0] > z[2])
        {
            double s = Math.sqrt(1.0 + x[0] - y[1] - z[2]) * 2.0;
            out[0] = 0.25 * s;
            out[1] = (y[0] + x[1]) / s;
            out[2] = (z[0] + x[2]) / s;
            out[3] = (y[2] - z[1]) / s;
        }
        else if (y[1] > z[2])
        {
            double s = Math.sqrt(1.0 + y[1] - x[0] - z[2]) * 2.0;
            out[0] = (y[0] + x[1]) / s;
            out[1] = 0.25 * s;
            out[2] = (z[1] + y[2]) / s;
            out[3] = (z[0] - x[2]) / s;
        }
        else
        {
            double s = Math.sqrt(1.0 + z[2] - x[0] - y[1]) * 2.0;
            out[0] = (z[0] + x[2]) / s;
            out[1] = (z[1] + y[2]) / s;
            out[2] = 0.25 * s;
            out[3] = (x[1] - y[0]) / s;
        }

        return normalize(out);
    }

    public static double[] mul(double[] a, double[] b, double[] out)
    {
        double x = a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1];
        double y = a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0];
        double z = a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3];
        double w = a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2];
        out[0] = x;
        out[1] = y;
        out[2] = z;
        out[3] = w;
        return out;
    }

    public static double[] conjugate(double[] q, double[] out)
    {
        out[0] = -q[0];
        out[1] = -q[1];
        out[2] = -q[2];
        out[3] = q[3];
        return out;
    }

    public static double[] normalize(double[] q)
    {
        double len = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (len < IDENTITY_EPSILON) return identity(q);

        double inv = 1.0 / len;
        q[0] *= inv;
        q[1] *= inv;
        q[2] *= inv;
        q[3] *= inv;
        return q;
    }

    public static double[] rotate(double[] q, double vx, double vy, double vz, double[] out)
    {
        double tx = 2.0 * (q[1] * vz - q[2] * vy);
        double ty = 2.0 * (q[2] * vx - q[0] * vz);
        double tz = 2.0 * (q[0] * vy - q[1] * vx);
        out[0] = vx + q[3] * tx + (q[1] * tz - q[2] * ty);
        out[1] = vy + q[3] * ty + (q[2] * tx - q[0] * tz);
        out[2] = vz + q[3] * tz + (q[0] * ty - q[1] * tx);
        return out;
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
