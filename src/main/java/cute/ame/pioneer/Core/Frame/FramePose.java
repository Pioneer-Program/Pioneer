package cute.ame.pioneer.Core.Frame;

import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class FramePose
{
    private final Vector3d p = new Vector3d();
    private final Vector3d r = new Vector3d();
    private final Quaterniond q = new Quaterniond();
    private final Quaterniond b = new Quaterniond();
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
                FrameQuat.rotate(b, p.x, p.y, p.z, p);
                FrameQuat.mul(b, q, q);
                rotated = true;
            }

            parent.positionAt(tick, partial, r);
            p.x += r.x;
            p.y += r.y;
            p.z += r.z;
        }

        px = p.x;
        py = p.y;
        pz = p.z;
        qx = q.x;
        qy = q.y;
        qz = q.z;
        qw = q.w;
        return this;
    }

    public Vector3d velocity(FrameMotion motion, FrameParent parent, boolean fixed, long tick, double partial, Vector3d dest)
    {
        motion.velocityAt(tick, partial, dest);
        if (parent == null) return dest;

        if (fixed)
        {
            parent.orientationAt(tick, partial, b);
            FrameQuat.rotate(b, dest.x, dest.y, dest.z, dest);
            motion.positionAt(tick, partial, p);
            FrameQuat.rotate(b, p.x, p.y, p.z, p);
            parent.spinAt(tick, r);
            dest.x += r.y * p.z - r.z * p.y;
            dest.y += r.z * p.x - r.x * p.z;
            dest.z += r.x * p.y - r.y * p.x;
        }

        parent.velocityAt(tick, partial, r);
        dest.x += r.x;
        dest.y += r.y;
        dest.z += r.z;
        return dest;
    }

    public Vector3d angularVelocity(FrameMotion motion, FrameParent parent, boolean fixed, long tick, Vector3d dest)
    {
        dest.set(motion.wx, motion.wy, motion.wz);
        if (parent == null || !fixed) return dest;

        parent.orientationAt(tick, 0.0, b);
        FrameQuat.rotate(b, dest.x, dest.y, dest.z, dest);
        parent.spinAt(tick, r);
        dest.x += r.x;
        dest.y += r.y;
        dest.z += r.z;
        return dest;
    }

    public FramePose identity()
    {
        px = py = pz = 0.0;
        qx = qy = qz = 0.0;
        qw = 1.0;
        rotated = false;
        return this;
    }

    public FrameMotion reparent(FrameMotion m, FrameParent from, boolean fromFixed, FrameParent to, boolean toFixed, long now)
    {
        m.rebase(now);
        Vector3d v = velocity(m, from, fromFixed, now, 0.0, new Vector3d());
        Vector3d w = new Vector3d(m.wx, m.wy, m.wz);
        Vector3d a = new Vector3d(m.ax, m.ay, m.az);
        Quaterniond turn = new Quaterniond();
        if (fromFixed && from != null)
        {
            from.orientationAt(now, 0.0, turn);
            FrameQuat.rotate(turn, w.x, w.y, w.z, w);
            FrameQuat.rotate(turn, a.x, a.y, a.z, a);
        }

        set(m, from, fromFixed, now, 0.0);
        Vector3d o = new Vector3d(px, py, pz);
        Quaterniond t = orientation(new Quaterniond());
        if (to != null)
        {
            Vector3d c = to.positionAt(now, 0.0, new Vector3d());
            Vector3d u = to.velocityAt(now, 0.0, new Vector3d());
            o.x -= c.x;
            o.y -= c.y;
            o.z -= c.z;
            v.x -= u.x;
            v.y -= u.y;
            v.z -= u.z;
            if (toFixed)
            {
                Vector3d s = to.spinAt(now, new Vector3d());
                v.x -= s.y * o.z - s.z * o.y;
                v.y -= s.z * o.x - s.x * o.z;
                v.z -= s.x * o.y - s.y * o.x;

                to.orientationAt(now, 0.0, turn).conjugate(turn);
                FrameQuat.rotate(turn, o.x, o.y, o.z, o);
                FrameQuat.rotate(turn, v.x, v.y, v.z, v);
                FrameQuat.rotate(turn, w.x, w.y, w.z, w);
                FrameQuat.rotate(turn, a.x, a.y, a.z, a);
                FrameQuat.normalize(FrameQuat.mul(turn, t, t));
            }
        }

        return m.set(now, o.x, o.y, o.z, v.x, v.y, v.z, a.x, a.y, a.z).orient(t.x, t.y, t.z, t.w).spin(w.x, w.y, w.z);
    }

    public Vector3d toSystem(double ox, double oy, double oz, Vector3d dest)
    {
        directionToSystem(ox, oy, oz, dest);
        dest.x += px;
        dest.y += py;
        dest.z += pz;
        return dest;
    }

    public Vector3d toLocal(double sx, double sy, double sz, Vector3d dest)
    {
        return directionToLocal(sx - px, sy - py, sz - pz, dest);
    }

    public Vector3d directionToSystem(double x, double y, double z, Vector3d dest)
    {
        if (!rotated) return dest.set(x, y, z);

        return FrameQuat.rotate(q.set(qx, qy, qz, qw), x, y, z, dest);
    }

    public Vector3d directionToLocal(double x, double y, double z, Vector3d dest)
    {
        if (!rotated) return dest.set(x, y, z);

        return FrameQuat.rotate(q.set(-qx, -qy, -qz, qw), x, y, z, dest);
    }

    public Quaterniond orientation(Quaterniond dest)
    {
        return dest.set(qx, qy, qz, qw);
    }
}
