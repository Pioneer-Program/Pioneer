package cute.ame.pioneer.SkyPlanet.Orbit;

import cute.ame.pioneer.SkyPlanet.Orbit.OrbitalElements;
import org.joml.Vector3d;
import org.joml.Matrix3d;

/**
 * The Hill equations, also known as the Clohessy-Wiltshire equations,
 * are applicable when the main and secondary bodies are in circular orbits.
 * The LVLH frame of the main vehicle is called the Hill or CW frame in this case.
 *
 * @param r - position vector, [rx, ry, rz]
 * @param v - velocity vector, [vx, vy, vz]
 */
public class HillEquations
{
    public class RelativeState
    {
        public Vector3d r;
        public Vector3d v;

        public RelativeState(Vector3d r, Vector3d v)
        {
            this.r = r;
            this.v = v;
        }
    }

    /**
     *
     * @param state - position & velocity of secondary body
     * @param mainOrbit - orbital elements of the main body
     * @param dt - propagation time
     */
    public void HillEOM(RelativeState state, OrbitalElements mainOrbit, double dt)
    {
        /* precalculate constants for speed */
        double n = mainOrbit.meanMotion(); // aka angular velocity
        double invn = 1 / n;
        double nt = n * dt;
        double sin = Math.sin(nt);
        double cos = Math.cos(nt);

        RelativeState temp = new RelativeState(null, null);

        /* Hill equations:
         * r = phiRR*r0 + phiRV*v0
         * v = phiVR*r0 + phiVV*v0
         */
        temp.r = state.r.mul(phiRR(nt, sin, cos)).add(state.v.mul(phiRV(nt, sin, cos, invn)));
        temp.v = state.r.mul(phiVR(nt, sin, cos, n)).add(state.v.mul(phiVV(nt, sin, cos)));

        state.r = temp.r;
        state.v = temp.v;
    }

    /**
     * State transition matrix relating new position to old position
     *
     * @param nt - product of mean motion and delta-time
     * @param sin - sin of nt
     * @param cos - cos of nt
     * @return 3x3 phi matrix
     */
    public Matrix3d phiRR(double nt, double sin, double cos)
    {
        return new Matrix3d(
            4 - (3 * cos), 0, 0,
            6 * (sin - nt), 1, 0,
            0, 0, cos
        );
    }

    /**
     * State transition matrix relating new position to old velocity
     *
     * @param nt - product of mean motion and delta-time
     * @param sin - sin of nt
     * @param cos - cos of nt
     * @param invn - inverse of mean motion
     * @return 3x3 phi matrix
     */
    public Matrix3d phiRV(double nt, double sin, double cos, double invn)
    {
        return new Matrix3d(
            invn * sin,             2 * invn * (1 - cos), 0,
            2 * invn * (cos - 1),   invn * ((4 * sin) - (3 * nt)), 0,
            0, 0, invn * sin
        );
    }

    /**
     * State transition matrix relating new velocity to old position
     *
     * @param nt - product of mean motion and delta-time
     * @param sin - sin of nt
     * @param cos - cos of nt
     * @param n - mean motion
     * @return 3x3 phi matrix
     */
    public Matrix3d phiVR(double nt, double sin, double cos, double n)
    {
        return new Matrix3d(
            3 * n * sin, 0, 0,
            6 * n * (cos - 1), 1, 0,
            0, 0, -n * sin
        );
    }

    /**
     * State transition matrix relating new velocity to old velocity
     *
     * @param nt - product of mean motion and delta-time
     * @param sin - sin of nt
     * @param cos - cos of nt
     * @return 3x3 phi matrix
     */
    public Matrix3d phiVV(double nt, double sin, double cos)
    {
        return new Matrix3d(
            cos,        2 * sin, 0,
            -2 * sin,   (4 * cos) - 3, 0,
            0, 0, cos
        );
    }
}
