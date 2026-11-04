package cute.ame.pioneer.Core.Frame;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class FrameShift
{
    private final Quaterniond q = new Quaterniond();
    private final Quaterniond a = new Quaterniond();
    private final Vector3d v = new Vector3d();
    public double tx, ty, tz;
    public double qx, qy, qz, qw = 1.0;
    public double lx, ly, lz;
    public double wx, wy, wz;
    public double cx, cy, cz;
    public boolean rotated;

    public FrameShift translation(double dx, double dy, double dz, double kickX, double kickY, double kickZ)
    {
        tx = dx;
        ty = dy;
        tz = dz;
        qx = qy = qz = 0.0;
        qw = 1.0;
        rotated = false;
        lx = kickX;
        ly = kickY;
        lz = kickZ;
        wx = wy = wz = 0.0;
        cx = cy = cz = 0.0;
        return this;
    }

    public FrameShift between(FramePose from, double fromX, double fromY, double fromZ, Vector3dc fromVelocity, Vector3dc fromSpin, FramePose to, double toX, double toY, double toZ, Vector3dc toVelocity, Vector3dc toSpin)
    {
        double dx = from.px - to.px, dy = from.py - to.py, dz = from.pz - to.pz;
        to.orientation(a).conjugate(a);
        FrameQuat.normalize(FrameQuat.mul(a, from.orientation(q), q));
        qx = q.x;
        qy = q.y;
        qz = q.z;
        qw = q.w;
        rotated = !FrameQuat.isIdentity(qx, qy, qz);

        FrameQuat.rotate(a, dx, dy, dz, v);
        tx = toX + v.x;
        ty = toY + v.y;
        tz = toZ + v.z;
        direction(fromX, fromY, fromZ, v);
        tx -= v.x;
        ty -= v.y;
        tz -= v.z;

        double sx = fromSpin.x(), sy = fromSpin.y(), sz = fromSpin.z();
        double kx = fromVelocity.x() - toVelocity.x() - (sy * dz - sz * dy);
        double ky = fromVelocity.y() - toVelocity.y() - (sz * dx - sx * dz);
        double kz = fromVelocity.z() - toVelocity.z() - (sx * dy - sy * dx);
        FrameQuat.rotate(a, kx, ky, kz, v);
        lx = v.x;
        ly = v.y;
        lz = v.z;

        FrameQuat.rotate(a, sx - toSpin.x(), sy - toSpin.y(), sz - toSpin.z(), v);
        wx = v.x;
        wy = v.y;
        wz = v.z;
        cx = toX;
        cy = toY;
        cz = toZ;
        return this;
    }

    public FrameShift identity()
    {
        return translation(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    public FrameShift set(FrameShift o)
    {
        tx = o.tx;
        ty = o.ty;
        tz = o.tz;
        qx = o.qx;
        qy = o.qy;
        qz = o.qz;
        qw = o.qw;
        lx = o.lx;
        ly = o.ly;
        lz = o.lz;
        wx = o.wx;
        wy = o.wy;
        wz = o.wz;
        cx = o.cx;
        cy = o.cy;
        cz = o.cz;
        rotated = o.rotated;
        return this;
    }

    public FrameShift markRotation()
    {
        rotated = !FrameQuat.isIdentity(qx, qy, qz);
        return this;
    }

    public boolean sameAs(FrameShift o)
    {
        return tx == o.tx && ty == o.ty && tz == o.tz && qx == o.qx && qy == o.qy && qz == o.qz && qw == o.qw
            && lx == o.lx && ly == o.ly && lz == o.lz && wx == o.wx && wy == o.wy && wz == o.wz && cx == o.cx && cy == o.cy && cz == o.cz;
    }

    public boolean isIdentity()
    {
        return tx == 0.0 && ty == 0.0 && tz == 0.0 && !rotated && !kicks();
    }

    public FrameShift then(FrameShift next)
    {
        next.direction(tx, ty, tz, v);
        double ntx = v.x + next.tx, nty = v.y + next.ty, ntz = v.z + next.tz;
        next.direction(wx, wy, wz, v);
        double rwx = v.x, rwy = v.y, rwz = v.z;
        next.direction(cx, cy, cz, v);
        double ex = next.cx - next.tx - v.x, ey = next.cy - next.ty - v.y, ez = next.cz - next.tz - v.z;
        next.direction(lx, ly, lz, v);
        lx = v.x + next.lx + rwy * ez - rwz * ey;
        ly = v.y + next.ly + rwz * ex - rwx * ez;
        lz = v.z + next.lz + rwx * ey - rwy * ex;
        wx = rwx + next.wx;
        wy = rwy + next.wy;
        wz = rwz + next.wz;
        cx = next.cx;
        cy = next.cy;
        cz = next.cz;
        tx = ntx;
        ty = nty;
        tz = ntz;
        if (!next.rotated) return this;

        a.set(next.qx, next.qy, next.qz, next.qw);
        q.set(qx, qy, qz, qw);
        FrameQuat.normalize(FrameQuat.mul(a, q, q));
        qx = q.x;
        qy = q.y;
        qz = q.z;
        qw = q.w;
        return markRotation();
    }

    public FrameShift invert()
    {
        double ox = tx, oy = ty, oz = tz;
        qx = -qx;
        qy = -qy;
        qz = -qz;
        direction(cx - ox, cy - oy, cz - oz, v);
        cx = v.x;
        cy = v.y;
        cz = v.z;
        direction(ox, oy, oz, v);
        tx = -v.x;
        ty = -v.y;
        tz = -v.z;
        direction(lx, ly, lz, v);
        lx = -v.x;
        ly = -v.y;
        lz = -v.z;
        direction(wx, wy, wz, v);
        wx = -v.x;
        wy = -v.y;
        wz = -v.z;
        return this;
    }

    public FrameShift scaleKick(double factor)
    {
        lx *= factor;
        ly *= factor;
        lz *= factor;
        wx *= factor;
        wy *= factor;
        wz *= factor;
        return this;
    }

    public boolean kicks()
    {
        return lx != 0.0 || ly != 0.0 || lz != 0.0 || spins();
    }

    public boolean spins()
    {
        return wx != 0.0 || wy != 0.0 || wz != 0.0;
    }

    public Vector3d position(double x, double y, double z, Vector3d dest)
    {
        direction(x, y, z, dest);
        dest.x += tx;
        dest.y += ty;
        dest.z += tz;
        return dest;
    }

    public Vector3d direction(double x, double y, double z, Vector3d dest)
    {
        if (!rotated) return dest.set(x, y, z);

        return FrameQuat.rotate(q.set(qx, qy, qz, qw), x, y, z, dest);
    }

    public Quaterniond orientation(Quaterniondc orientation, Quaterniond dest)
    {
        dest.set(orientation);
        if (!rotated) return dest;

        return FrameQuat.normalize(FrameQuat.mul(q.set(qx, qy, qz, qw), dest, dest));
    }

    public Vector3d kick(double x, double y, double z, Vector3d dest)
    {
        double ox = x - cx, oy = y - cy, oz = z - cz;
        return dest.set(lx + wy * oz - wz * oy, ly + wz * ox - wx * oz, lz + wx * oy - wy * ox);
    }

    public double[] bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double[] out)
    {
        double hx = 0.5 * (maxX - minX), hy = 0.5 * (maxY - minY), hz = 0.5 * (maxZ - minZ);
        position(0.5 * (minX + maxX), 0.5 * (minY + maxY), 0.5 * (minZ + maxZ), v);
        double ex = hx, ey = hy, ez = hz;
        if (rotated)
        {
            double xx = qx * qx, yy = qy * qy, zz = qz * qz;
            double xy = qx * qy, xz = qx * qz, yz = qy * qz, xw = qx * qw, yw = qy * qw, zw = qz * qw;
            ex = Math.abs(1.0 - 2.0 * (yy + zz)) * hx + Math.abs(2.0 * (xy - zw)) * hy + Math.abs(2.0 * (xz + yw)) * hz;
            ey = Math.abs(2.0 * (xy + zw)) * hx + Math.abs(1.0 - 2.0 * (xx + zz)) * hy + Math.abs(2.0 * (yz - xw)) * hz;
            ez = Math.abs(2.0 * (xz - yw)) * hx + Math.abs(2.0 * (yz + xw)) * hy + Math.abs(1.0 - 2.0 * (xx + yy)) * hz;
        }

        out[0] = v.x - ex;
        out[1] = v.y - ey;
        out[2] = v.z - ez;
        out[3] = v.x + ex;
        out[4] = v.y + ey;
        out[5] = v.z + ez;
        return out;
    }

    public Vector3d look(float yaw, float pitch, Vector3d dest)
    {
        FrameLook.direction(yaw, pitch, dest);
        return direction(dest.x, dest.y, dest.z, dest);
    }
}
