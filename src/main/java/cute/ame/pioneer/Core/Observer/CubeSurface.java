package cute.ame.pioneer.Core.Observer;

import org.joml.Vector3d;

public final class CubeSurface
{
    public static final int FACE_FRONT = 0, FACE_BACK = 1, FACE_LEFT = 2, FACE_RIGHT = 3, FACE_TOP = 4, FACE_BOTTOM = 5;
    public static final int EDGE_PLUS_U = 0, EDGE_MINUS_U = 1, EDGE_PLUS_V = 2, EDGE_MINUS_V = 3;

    private static final double[][] U =
    {
        {-1, 0, 0}, // FRONT (+Z)
        { 1, 0, 0}, // BACK (-Z)
        { 0, 0,-1}, // LEFT (-X)
        { 0, 0, 1}, // RIGHT (+X)
        { 1, 0, 0}, // TOP (+Y)
        { 1, 0, 0} // BOTTOM (-Y)
    };
    private static final double[][] V =
    {
        { 0,-1, 0}, { 0,-1, 0}, { 0,-1, 0}, { 0,-1, 0},
        { 0, 0,-1}, // TOP
        { 0, 0, 1} // BOTTOM
    };

    private static final double[][] W =
    {
        { 0, 0, 1}, { 0, 0,-1}, {-1, 0, 0}, { 1, 0, 0}, { 0, 1, 0}, { 0,-1, 0}
    };

    private static final double FACE_STRIDE = 4.0;

    public record Crossing(int face, double blockX, double blockZ, double u, double v, int exitEdge, int entryEdge) {}

    public static boolean areAdjacent(int a, int b)
    {
        double[] wa = W[a], wb = W[b];
        return Math.abs(wa[0] * wb[0] + wa[1] * wb[1] + wa[2] * wb[2]) < 0.5;
    }

    public static double halfSide(double radiusKm, double metresPerBlock)
    {
        return Math.max(radiusKm, 1.0e-3) * 1000.0 / Math.max(metresPerBlock, 1.0e-6);
    }

    public static double faceOriginX(double halfSide, int face)
    {
        return face * FACE_STRIDE * halfSide;
    }

    public static int faceOf(double halfSide, double blockX)
    {
        return Math.clamp((int) Math.round(blockX / (FACE_STRIDE * halfSide)), 0, 5);
    }

    public static double toU(double halfSide, int face, double blockX)
    {
        return (blockX - faceOriginX(halfSide, face)) / halfSide;
    }

    public static double toV(double halfSide, double blockZ)
    {
        return blockZ / halfSide;
    }

    public static double[] normal(int face)
    {
        return W[face];
    }

    public static void north(int face, double[] out)
    {
        double[] b = V[face];
        out[0] = -b[0]; out[1] = -b[1]; out[2] = -b[2];
    }

    public static Vector3d toDirection(int face, double u, double v, Vector3d dest)
    {
        double[] a = U[face], b = V[face], c = W[face];
        return dest.set(a[0] * u + b[0] * v + c[0], a[1] * u + b[1] * v + c[1], a[2] * u + b[2] * v + c[2]).normalize();
    }

    public static Vector3d facePointKm(double halfExtentKm, int face, double u, double v, double altKm, Vector3d dest)
    {
        double[] a = U[face], b = V[face], c = W[face];
        double h = halfExtentKm;
        return dest.set(h * (a[0] * u + b[0] * v + c[0]) + altKm * c[0], h * (a[1] * u + b[1] * v + c[1]) + altKm * c[1], h * (a[2] * u + b[2] * v + c[2]) + altKm * c[2]);
    }

    public static int fromDirection(Vector3d dir, double[] uvOut)
    {
        double ax = Math.abs(dir.x), ay = Math.abs(dir.y), az = Math.abs(dir.z);
        int face;
        double m;
        if (ax >= ay && ax >= az)
        {
            face = dir.x > 0 ? FACE_RIGHT : FACE_LEFT;
            m = ax;
        }
        else if (ay >= az)
        {
            face = dir.y > 0 ? FACE_TOP : FACE_BOTTOM;
            m = ay;
        }
        else
        {
            face = dir.z > 0 ? FACE_FRONT : FACE_BACK;
            m = az;
        }

        double inv = 1.0 / m;
        double[] a = U[face], b = V[face];

        uvOut[0] = (a[0] * dir.x + a[1] * dir.y + a[2] * dir.z) * inv;
        uvOut[1] = (b[0] * dir.x + b[1] * dir.y + b[2] * dir.z) * inv;
        return face;
    }

    public static Crossing cross(double halfSide, int face, double u, double v)
    {
        boolean outU = Math.abs(u) > 1.0, outV = Math.abs(v) > 1.0;
        if (outU == outV) return null;

        int exitEdge = outU ? (u > 0 ? EDGE_PLUS_U : EDGE_MINUS_U) : (v > 0 ? EDGE_PLUS_V : EDGE_MINUS_V);
        double[] uv = new double[2];
        int nf = fromDirection(toDirection(face, u, v, new Vector3d()), uv);
        if (nf == face) return null;

        return new Crossing(nf, faceOriginX(halfSide, nf) + uv[0] * halfSide, uv[1] * halfSide, uv[0], uv[1], exitEdge, entryEdge(face, nf));
    }

    public static int entryEdge(int from, int to)
    {
        double[] wf = W[from], ut = U[to], vt = V[to];
        double du = wf[0] * ut[0] + wf[1] * ut[1] + wf[2] * ut[2];
        if (du > 0.5) return EDGE_PLUS_U;
        if (du < -0.5) return EDGE_MINUS_U;
        double dv = wf[0] * vt[0] + wf[1] * vt[1] + wf[2] * vt[2];
        return dv > 0.0 ? EDGE_PLUS_V : EDGE_MINUS_V;
    }

    public static void transportHeading(int from, int to, double du, double dv, double[] out)
    {
        double[] uf = U[from], vf = V[from], wf = W[from], wt = W[to];

        double tx = uf[0] * du + vf[0] * dv;
        double ty = uf[1] * du + vf[1] * dv;
        double tz = uf[2] * du + vf[2] * dv;

        double a = tx * wt[0] + ty * wt[1] + tz * wt[2];
        double nx = tx - a * wt[0] - a * wf[0];
        double ny = ty - a * wt[1] - a * wf[1];
        double nz = tz - a * wt[2] - a * wf[2];

        double[] ut = U[to], vt = V[to];
        out[0] = nx * ut[0] + ny * ut[1] + nz * ut[2];
        out[1] = nx * vt[0] + ny * vt[1] + nz * vt[2];
    }

    public static float yawOf(double du, double dv)
    {
        return (float) Math.toDegrees(Math.atan2(-du, dv));
    }

    public static double wrapDegrees(double deg)
    {
        double d = (deg + 180.0) % 360.0;
        if (d < 0.0) d += 360.0;
        return d - 180.0;
    }
}