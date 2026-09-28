package Orbit;

import cute.ame.pioneer.SkyPlanet.Data.OrbitDefinition;
import cute.ame.pioneer.SkyPlanet.Orbit.OrbitalElements;
import cute.ame.pioneer.SkyPlanet.Orbit.HillEquations;

import org.joml.Vector3d;
import org.joml.Matrix3d;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestRelativeMotion {

    @Test
    void example7_4() {
        // Example 7.4, Orbital Mechanics for Engineering Students by Howard Curtis
        double n = 0.00115691; // rad/s
        double dt = 8.0 * 3600.0; // hours
        double nt = n*dt;
        double s = Math.sin(nt);
        double c = Math.cos(nt);

        Matrix3d phiRR = HillEquations.phiRR(nt, s, c);
        Matrix3d phiRV = HillEquations.phiRV(nt, s, c, 1.0/n);
        Matrix3d phiVR = HillEquations.phiVR(nt, s, c, n);
        Matrix3d phiVV = HillEquations.phiVV(nt, s, c);

        double tol = 1.0e-6;
        assertEquals(4.97849, phiRR.m00, tol);
        assertEquals(0.0, phiRR.m01, tol);
        assertEquals(0.0, phiRR.m02, tol);
        assertEquals(-194.242, phiRR.m10, tol);
        assertEquals(1.0, phiRR.m11, tol);
        assertEquals(0.0, phiRR.m12, tol);
        assertEquals(0.0, phiRR.m20, tol);
        assertEquals(0.0, phiRR.m21, tol);
        assertEquals(-0.326163, phiRR.m22, tol);
    }

	@Test
	void addition() {
        // Problem 7.7, Orbital Mechanics for Engineering Students by Howard Curtis
        // Main body is in 90-min period earth orbit.
        // Secondary body has dr = [1, 0, 0] km, dv = [0, 10, 0] m/s.
        // 15 min later, magnitude of dr should be 11.2 km
        double mu = 3.986e14; // earth gravitational parameter, m^3/s^2
        double P = 1.5 / 24.0; // period, days
        double a = OrbitalElements.semiMajorFromPeriod(P, mu);
        OrbitDefinition odef = new OrbitDefinition(a, P, 0.0, 0.0, 0.0, 0.0, 0.0);
	    OrbitalElements coe = OrbitalElements.fromOrbitDefinition(odef, mu);

        Vector3d r = new Vector3d(1.0, 0.0, 0.0); // km
        Vector3d v = new Vector3d(0.0, 10.0, 0.0); // m/s
        HillEquations.RelativeState state = new HillEquations.RelativeState(r, v);

        double dt = 15.0 * 60.0; // sec
        HillEquations.HillEOM(state, coe, dt);
        double rmag = state.r.length();

		assertEquals(11.2, rmag);
	}

    @Test
    void test2() {
        // Problem 7.8, Orbital Mechanics for Engineering Students by Howard Curtis
        // A and B are in the same circular earth orbit with a period of 2 h.
        // B is 6 km ahead of A.
        // At t = 0, B applies an in-track delta-v (retrofire) of 3 m/s.
        // Using a CW frame attached to A:
        // 1. determine the distance between A and B at t = 30 min (dr = 10.9 km)
        // 2. and the velocity of B relative to A. (dv = 10.8 m/s)
    }
}