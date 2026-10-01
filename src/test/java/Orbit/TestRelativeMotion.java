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

        double ftol = 1.0e-6;
        double tol = 1.0e-3;

        // PhiRR
        assertEquals(4.978, phiRR.m00, tol);
        assertEquals(0.0, phiRR.m01, ftol);
        assertEquals(0.0, phiRR.m02, ftol);
        assertEquals(-194.242, phiRR.m10, tol);
        assertEquals(1.0, phiRR.m11, tol);
        assertEquals(0.0, phiRR.m12, ftol);
        assertEquals(0.0, phiRR.m20, ftol);
        assertEquals(0.0, phiRR.m21, ftol);
        assertEquals(-0.326, phiRR.m22, tol);

        // PhiRV
        assertEquals(817.1, phiRV.m00, 0.1);
        assertEquals(2292.6, phiRV.m01, 0.1);
        assertEquals(0.0, phiRV.m02, ftol);
        assertEquals(-2292.6, phiRV.m10, 0.1);
        assertEquals(-83131.6, phiRV.m11, 0.1);
        assertEquals(0.0, phiRV.m12, ftol);
        assertEquals(0.0, phiRV.m20, ftol);
        assertEquals(0.0, phiRV.m21, ftol);
        assertEquals(817.103, phiRV.m22, 0.1);

        // PhiVR
        assertEquals(0.00328092, phiVR.m00, ftol);
        assertEquals(0.0, phiVR.m01, ftol);
        assertEquals(0.0, phiVR.m02, ftol);
        assertEquals(-0.00920550, phiVR.m10, ftol);
        assertEquals(0.0, phiVR.m11, ftol);
        assertEquals(0.0, phiVR.m12, ftol);
        assertEquals(0.0, phiVR.m20, ftol);
        assertEquals(0.0, phiVR.m21, ftol);
        assertEquals(-0.00109364, phiVR.m22, ftol);

        // PhiVV
        assertEquals(-0.326, phiVV.m00, tol);
        assertEquals(1.8906, phiVV.m01, 1.0e-4);
        assertEquals(0.0, phiVV.m02, ftol);
        assertEquals(-1.8906, phiVV.m10, 1.0e-4);
        assertEquals(-4.305, phiVV.m11, tol);
        assertEquals(0.0, phiVV.m12, ftol);
        assertEquals(0.0, phiVV.m20, ftol);
        assertEquals(0.0, phiVV.m21, ftol);
        assertEquals(-0.3262, phiVV.m22, 1.0e-4);
    }

	@Test
	void problem7_7() {
        // Problem 7.7, Orbital Mechanics for Engineering Students by Howard Curtis
        // Main body is in 90-min period earth orbit.
        // Secondary body has dr = [1, 0, 0] km, dv = [0, 10, 0] m/s.
        // 15 min later, magnitude of dr should be 11.2 km
        double mu_km = 398_600; // earth gravitational parameter, km^3/s^2
        double P_days = 1.5 / 24.0; // period, days
        double a_km = OrbitalElements.semiMajorFromPeriod(P_days, mu_km);
        OrbitDefinition odef_km = new OrbitDefinition(a_km, P_days, 0.0, 0.0, 0.0, 0.0, 0.0);
	    OrbitalElements coe_km = OrbitalElements.fromOrbitDefinition(odef_km, mu_km);

        Vector3d r = new Vector3d(1.0, 0.0, 0.0); // km
        Vector3d v = new Vector3d(0.0, 10.0e-3, 0.0); // km/s
        HillEquations.RelativeState state = new HillEquations.RelativeState(r, v);

        double dt = 15.0 * 60.0; // sec
        HillEquations.HillEOM(state, coe_km, dt);
        double rmag_km = state.r.length();

		assertEquals(11.2, rmag_km, 0.5e-2);
	}

    @Test
    void problem7_8() {
        // Problem 7.8, Orbital Mechanics for Engineering Students by Howard Curtis
        // A and B are in the same circular earth orbit with a period of 2 h.
        // B is 6 km ahead of A.
        // At t = 0, B applies an in-track delta-v (retrofire) of 3 m/s.
        // Using a CW frame attached to A:
        // 1. determine the distance between A and B at t = 30 min (dr = 10.9 km)
        // 2. and the velocity of B relative to A. (dv = 10.8 m/s)

        double mu = 398_600; // earth gravitational parameter, km^3/s^2
        double P = 2.0 / 24.0; // period, days
        double dt = 30.0; // propagation time, sec
        double a = OrbitalElements.semiMajorFromPeriod(P, mu); // length matches units on mu
        OrbitDefinition odef = new OrbitDefinition(a, P, 0.0, 0.0, 0.0, 0.0, 0.0);
	    OrbitalElements coe = OrbitalElements.fromOrbitDefinition(odef, mu);

        // y-axis aka vbar is in-track
        Vector3d r = new Vector3d(0.0, 6.0, 0.0); // km
        Vector3d v = new Vector3d(0.0, -3.0e-3, 0.0); // km/s
        HillEquations.RelativeState state = new HillEquations.RelativeState(r, v);

        HillEquations.HillEOM(state, coe, dt);
        double rmag = state.r.length();
        double vmag = state.v.length();

		assertEquals(10.9, rmag, 0.5e-2);
		assertEquals(10.8, vmag, 0.5e-2);
    }
}