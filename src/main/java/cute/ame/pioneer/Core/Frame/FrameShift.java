package cute.ame.pioneer.Core.Frame;

public final class FrameShift
{
    private final double[] q = new double[4];
    private final double[] a = new double[4];
    private final double[] v = new double[3];
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

    public FrameShift between(FramePose from, double fromX, double fromY, double fromZ, double[] fromVelocity, double[] fromSpin, FramePose to, double toX, double toY, double toZ, double[] toVelocity, double[] toSpin)
    {
        double dx = from.px - to.px, dy = from.py - to.py, dz = from.pz - to.pz;
        to.orientation(a);
        FrameQuat.conjugate(a, a);
        FrameQuat.normalize(FrameQuat.mul(a, from.orientation(q), q));
        qx = q[0];
        qy = q[1];
        qz = q[2];
        qw = q[3];
        rotated = !FrameQuat.isIdentity(qx, qy, qz);

        FrameQuat.rotate(a, dx, dy, dz, v);
        tx = toX + v[0];
        ty = toY + v[1];
        tz = toZ + v[2];
        direction(fromX, fromY, fromZ, v);
        tx -= v[0];
        ty -= v[1];
        tz -= v[2];

        double sx = fromSpin[0], sy = fromSpin[1], sz = fromSpin[2];
        double kx = fromVelocity[0] - toVelocity[0] - (sy * dz - sz * dy);
        double ky = fromVelocity[1] - toVelocity[1] - (sz * dx - sx * dz);
        double kz = fromVelocity[2] - toVelocity[2] - (sx * dy - sy * dx);
        FrameQuat.rotate(a, kx, ky, kz, v);
        lx = v[0];
        ly = v[1];
        lz = v[2];

        FrameQuat.rotate(a, sx - toSpin[0], sy - toSpin[1], sz - toSpin[2], v);
        wx = v[0];
        wy = v[1];
        wz = v[2];
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
        double ntx = v[0] + next.tx, nty = v[1] + next.ty, ntz = v[2] + next.tz;
        next.direction(wx, wy, wz, v);
        double rwx = v[0], rwy = v[1], rwz = v[2];
        next.direction(cx, cy, cz, v);
        double ex = next.cx - next.tx - v[0], ey = next.cy - next.ty - v[1], ez = next.cz - next.tz - v[2];
        next.direction(lx, ly, lz, v);
        lx = v[0] + next.lx + rwy * ez - rwz * ey;
        ly = v[1] + next.ly + rwz * ex - rwx * ez;
        lz = v[2] + next.lz + rwx * ey - rwy * ex;
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

        a[0] = next.qx;
        a[1] = next.qy;
        a[2] = next.qz;
        a[3] = next.qw;
        q[0] = qx;
        q[1] = qy;
        q[2] = qz;
        q[3] = qw;
        FrameQuat.normalize(FrameQuat.mul(a, q, q));
        qx = q[0];
        qy = q[1];
        qz = q[2];
        qw = q[3];
        return markRotation();
    }

    public FrameShift invert()
    {
        double ox = tx, oy = ty, oz = tz;
        qx = -qx;
        qy = -qy;
        qz = -qz;
        direction(cx - ox, cy - oy, cz - oz, v);
        cx = v[0];
        cy = v[1];
        cz = v[2];
        direction(ox, oy, oz, v);
        tx = -v[0];
        ty = -v[1];
        tz = -v[2];
        direction(lx, ly, lz, v);
        lx = -v[0];
        ly = -v[1];
        lz = -v[2];
        direction(wx, wy, wz, v);
        wx = -v[0];
        wy = -v[1];
        wz = -v[2];
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

    public double[] position(double x, double y, double z, double[] out)
    {
        direction(x, y, z, out);
        out[0] += tx;
        out[1] += ty;
        out[2] += tz;
        return out;
    }

    public double[] direction(double x, double y, double z, double[] out)
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

    public double[] orientation(double x, double y, double z, double w, double[] out)
    {
        out[0] = x;
        out[1] = y;
        out[2] = z;
        out[3] = w;
        if (!rotated) return out;

        q[0] = qx;
        q[1] = qy;
        q[2] = qz;
        q[3] = qw;
        return FrameQuat.normalize(FrameQuat.mul(q, out, out));
    }

    public double[] kick(double x, double y, double z, double[] out)
    {
        double ox = x - cx, oy = y - cy, oz = z - cz;
        out[0] = lx + wy * oz - wz * oy;
        out[1] = ly + wz * ox - wx * oz;
        out[2] = lz + wx * oy - wy * ox;
        return out;
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

        out[0] = v[0] - ex;
        out[1] = v[1] - ey;
        out[2] = v[2] - ez;
        out[3] = v[0] + ex;
        out[4] = v[1] + ey;
        out[5] = v[2] + ez;
        return out;
    }

    public double[] look(float yaw, float pitch, double[] out)
    {
        FrameLook.direction(yaw, pitch, out);
        direction(out[0], out[1], out[2], out);
        double x = out[0], y = out[1], z = out[2];
        out[0] = FrameLook.yaw(x, z, yaw);
        out[1] = FrameLook.pitch(x, y, z);
        return out;
    }
}
