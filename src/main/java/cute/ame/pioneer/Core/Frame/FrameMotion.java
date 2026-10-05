package cute.ame.pioneer.Core.Frame;

public final class FrameMotion
{
    public long epoch;
    public double px, py, pz;
    public double vx, vy, vz;
    public double ax, ay, az;
    public double qx, qy, qz, qw = 1.0;
    public double wx, wy, wz;

    public FrameMotion set(long epoch, double px, double py, double pz, double vx, double vy, double vz, double ax, double ay, double az)
    {
        this.epoch = epoch;
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        return this;
    }

    public FrameMotion set(FrameMotion o)
    {
        set(o.epoch, o.px, o.py, o.pz, o.vx, o.vy, o.vz, o.ax, o.ay, o.az);
        return orient(o.qx, o.qy, o.qz, o.qw).spin(o.wx, o.wy, o.wz);
    }

    public FrameMotion orient(double qx, double qy, double qz, double qw)
    {
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
        return this;
    }

    public FrameMotion spin(double wx, double wy, double wz)
    {
        this.wx = wx;
        this.wy = wy;
        this.wz = wz;
        return this;
    }

    public boolean spinning()
    {
        return wx != 0.0 || wy != 0.0 || wz != 0.0;
    }

    public boolean rotated()
    {
        return spinning() || !FrameQuat.isIdentity(qx, qy, qz);
    }

    public double spinRate()
    {
        return Math.sqrt(wx * wx + wy * wy + wz * wz);
    }

    public double[] orientationAt(long tick, double partial, double[] out)
    {
        out[0] = qx;
        out[1] = qy;
        out[2] = qz;
        out[3] = qw;
        if (!spinning()) return out;

        double rate = spinRate();
        double half = 0.5 * rate * dt(tick, partial);
        double s = Math.sin(half) / rate, c = Math.cos(half);
        double rx = wx * s, ry = wy * s, rz = wz * s;
        out[0] = c * qx + rx * qw + ry * qz - rz * qy;
        out[1] = c * qy - rx * qz + ry * qw + rz * qx;
        out[2] = c * qz + rx * qy - ry * qx + rz * qw;
        out[3] = c * qw - rx * qx - ry * qy - rz * qz;
        return FrameQuat.normalize(out);
    }

    public double dt(long tick, double partial)
    {
        return (double) (tick - epoch) + partial;
    }

    public double x(double dt)
    {
        return px + dt * (vx + 0.5 * ax * dt);
    }

    public double y(double dt)
    {
        return py + dt * (vy + 0.5 * ay * dt);
    }

    public double z(double dt)
    {
        return pz + dt * (vz + 0.5 * az * dt);
    }

    public double[] positionAt(long tick, double partial, double[] out)
    {
        double dt = dt(tick, partial);
        out[0] = x(dt);
        out[1] = y(dt);
        out[2] = z(dt);
        return out;
    }

    public double[] velocityAt(long tick, double partial, double[] out)
    {
        double dt = dt(tick, partial);
        out[0] = vx + ax * dt;
        out[1] = vy + ay * dt;
        out[2] = vz + az * dt;
        return out;
    }

    public double speedAt(long tick)
    {
        double dt = tick - epoch;
        double x = vx + ax * dt, y = vy + ay * dt, z = vz + az * dt;
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double accel()
    {
        return Math.sqrt(ax * ax + ay * ay + az * az);
    }

    public FrameMotion rebase(long tick)
    {
        if (tick == epoch) return this;

        if (spinning())
        {
            double[] q = orientationAt(tick, 0.0, new double[4]);
            orient(q[0], q[1], q[2], q[3]);
        }

        double dt = tick - epoch;
        px = x(dt);
        py = y(dt);
        pz = z(dt);
        vx += ax * dt;
        vy += ay * dt;
        vz += az * dt;
        epoch = tick;
        return this;
    }

    public FrameMotion accelerate(double ax, double ay, double az)
    {
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        return this;
    }

    public FrameMotion addVelocity(double dx, double dy, double dz)
    {
        vx += dx;
        vy += dy;
        vz += dz;
        return this;
    }

    public FrameMotion translate(double dx, double dy, double dz)
    {
        px += dx;
        py += dy;
        pz += dz;
        return this;
    }

    public FrameMotion recenter(long tick, double ox, double oy, double oz, double ux, double uy, double uz)
    {
        rebase(tick);
        if (rotated())
        {
            double[] q = {qx, qy, qz, qw}, o = new double[3], u = new double[3];
            FrameQuat.rotate(q, ox, oy, oz, o);
            FrameQuat.rotate(q, ux, uy, uz, u);
            ox = o[0];
            oy = o[1];
            oz = o[2];
            ux = u[0] + wy * oz - wz * oy;
            uy = u[1] + wz * ox - wx * oz;
            uz = u[2] + wx * oy - wy * ox;
        }

        return translate(ox, oy, oz).addVelocity(ux, uy, uz);
    }
}
