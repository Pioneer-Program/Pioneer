package cute.ame.pioneer.Core.Frame;

import org.joml.Quaterniond;
import org.joml.Vector3d;

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

    public Quaterniond orientationAt(long tick, double partial, Quaterniond dest)
    {
        if (!spinning()) return dest.set(qx, qy, qz, qw);

        double rate = spinRate();
        double half = 0.5 * rate * dt(tick, partial);
        double s = Math.sin(half) / rate, c = Math.cos(half);
        double rx = wx * s, ry = wy * s, rz = wz * s;
        dest.set(c * qx + rx * qw + ry * qz - rz * qy, c * qy - rx * qz + ry * qw + rz * qx, c * qz + rx * qy - ry * qx + rz * qw, c * qw - rx * qx - ry * qy - rz * qz);
        return FrameQuat.normalize(dest);
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

    public Vector3d positionAt(long tick, double partial, Vector3d dest)
    {
        double dt = dt(tick, partial);
        return dest.set(x(dt), y(dt), z(dt));
    }

    public Vector3d velocityAt(long tick, double partial, Vector3d dest)
    {
        double dt = dt(tick, partial);
        return dest.set(vx + ax * dt, vy + ay * dt, vz + az * dt);
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
            Quaterniond q = orientationAt(tick, 0.0, new Quaterniond());
            orient(q.x, q.y, q.z, q.w);
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
            Quaterniond q = new Quaterniond(qx, qy, qz, qw);
            Vector3d o = FrameQuat.rotate(q, ox, oy, oz, new Vector3d()), u = FrameQuat.rotate(q, ux, uy, uz, new Vector3d());
            ox = o.x;
            oy = o.y;
            oz = o.z;
            ux = u.x + wy * oz - wz * oy;
            uy = u.y + wz * ox - wx * oz;
            uz = u.z + wx * oy - wy * ox;
        }

        return translate(ox, oy, oz).addVelocity(ux, uy, uz);
    }
}
