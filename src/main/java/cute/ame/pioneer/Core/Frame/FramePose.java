package cute.ame.pioneer.Core.Frame;

public final class FramePose
{
    private final double[] p = new double[3];
    private final double[] q = new double[4];
    private final double[] b = new double[4];
    private final double[] r = new double[3];
    public double px, py, pz;
    public double qx, qy, qz, qw = 1.0;
    public boolean rotated;

    public FramePose set(FrameMotion motion, FrameParent parent, boolean fixed, long tick, double partial)
    {
        motion.positionAt(tick, partial, p);
        motion.orientationAt(tick, partial, q);
        rotated = motion.rotated();
        if (parent != null)
        {
            if (fixed)
            {
                parent.orientationAt(tick, partial, b);
                FrameQuat.rotate(b, p[0], p[1], p[2], p);
                FrameQuat.mul(b, q, q);
                rotated = true;
            }

            parent.positionAt(tick, partial, r);
            p[0] += r[0];
            p[1] += r[1];
            p[2] += r[2];
        }

        px = p[0];
        py = p[1];
        pz = p[2];
        qx = q[0];
        qy = q[1];
        qz = q[2];
        qw = q[3];
        return this;
    }

    public double[] velocity(FrameMotion motion, FrameParent parent, boolean fixed, long tick, double partial, double[] out)
    {
        motion.velocityAt(tick, partial, out);
        if (parent == null) return out;

        if (fixed)
        {
            parent.orientationAt(tick, partial, b);
            FrameQuat.rotate(b, out[0], out[1], out[2], out);
            motion.positionAt(tick, partial, p);
            FrameQuat.rotate(b, p[0], p[1], p[2], p);
            parent.spinAt(tick, r);
            out[0] += r[1] * p[2] - r[2] * p[1];
            out[1] += r[2] * p[0] - r[0] * p[2];
            out[2] += r[0] * p[1] - r[1] * p[0];
        }

        parent.velocityAt(tick, partial, r);
        out[0] += r[0];
        out[1] += r[1];
        out[2] += r[2];
        return out;
    }

    public FrameMotion reparent(FrameMotion m, FrameParent from, boolean fromFixed, FrameParent to, boolean toFixed, long now)
    {
        m.rebase(now);
        double[] v = velocity(m, from, fromFixed, now, 0.0, new double[3]);
        double[] w = {m.wx, m.wy, m.wz};
        double[] a = {m.ax, m.ay, m.az};
        double[] turn = new double[4];
        if (fromFixed && from != null)
        {
            from.orientationAt(now, 0.0, turn);
            FrameQuat.rotate(turn, w[0], w[1], w[2], w);
            FrameQuat.rotate(turn, a[0], a[1], a[2], a);
        }

        set(m, from, fromFixed, now, 0.0);
        double[] o = {px, py, pz};
        double[] t = orientation(new double[4]);
        if (to != null)
        {
            double[] c = to.positionAt(now, 0.0, new double[3]);
            double[] u = to.velocityAt(now, 0.0, new double[3]);
            for (int i = 0; i < 3; i++)
            {
                o[i] -= c[i];
                v[i] -= u[i];
            }

            if (toFixed)
            {
                double[] s = to.spinAt(now, new double[3]);
                v[0] -= s[1] * o[2] - s[2] * o[1];
                v[1] -= s[2] * o[0] - s[0] * o[2];
                v[2] -= s[0] * o[1] - s[1] * o[0];

                FrameQuat.conjugate(to.orientationAt(now, 0.0, turn), turn);
                FrameQuat.rotate(turn, o[0], o[1], o[2], o);
                FrameQuat.rotate(turn, v[0], v[1], v[2], v);
                FrameQuat.rotate(turn, w[0], w[1], w[2], w);
                FrameQuat.rotate(turn, a[0], a[1], a[2], a);
                FrameQuat.normalize(FrameQuat.mul(turn, t, t));
            }
        }

        return m.set(now, o[0], o[1], o[2], v[0], v[1], v[2], a[0], a[1], a[2]).orient(t[0], t[1], t[2], t[3]).spin(w[0], w[1], w[2]);
    }

    public double[] toSystem(double ox, double oy, double oz, double[] out)
    {
        directionToSystem(ox, oy, oz, out);
        out[0] += px;
        out[1] += py;
        out[2] += pz;
        return out;
    }

    public double[] toLocal(double sx, double sy, double sz, double[] out)
    {
        return directionToLocal(sx - px, sy - py, sz - pz, out);
    }

    public double[] directionToSystem(double x, double y, double z, double[] out)
    {
        if (!rotated)
        {
            out[0] = x;
            out[1] = y;
            out[2] = z;
            return out;
        }

        q[0] = qx;
        q[1] = qy;
        q[2] = qz;
        q[3] = qw;
        return FrameQuat.rotate(q, x, y, z, out);
    }

    public double[] directionToLocal(double x, double y, double z, double[] out)
    {
        if (!rotated)
        {
            out[0] = x;
            out[1] = y;
            out[2] = z;
            return out;
        }

        q[0] = -qx;
        q[1] = -qy;
        q[2] = -qz;
        q[3] = qw;
        return FrameQuat.rotate(q, x, y, z, out);
    }

    public double[] orientation(double[] out)
    {
        out[0] = qx;
        out[1] = qy;
        out[2] = qz;
        out[3] = qw;
        return out;
    }
}
