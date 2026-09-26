import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import cute.ame.pioneer.SkyPlanet.Data.OrbitDefinition;
import cute.ame.pioneer.SkyPlanet.Orbit.OrbitalElements;
import cute.ame.pioneer.SkyPlanet.Orbit.HillEquations;

class TestRelativeMotion {

	@Test
	void addition() {
        // Main body is in 90-min period earth orbit. 
        // Secondary body has dr = [1000, 0, 0] m, dv = [0, 10, 0] m/s. 
        // 15 min later, magnitude of dr should be 11.2 km
        double mu = 3.986e14; // earth gravitational parameter, m^3/s^2
        double P = 1.5 / 24.0; // period, days
        double a = OrbitalElements.semiMajorFromPeriod(P, mu);
        OrbitDefinition odef = new OrbitDefinition(a, P);
	    OrbitalElements coe = new OrbitalElements(odef, mu);

        

		assertEquals(2, calculator.add(1, 1));
	}

    @Test
    void test2() {
        // 7.8 Spacecraft A and B are in the same circular earth orbit with a period of 2 h. B is 6 km ahead of A.
        // At t ¼ 0, B applies an in-track delta-v (retrofire) of 3 m/s. Using a CW frame attached to A,
        // determine the distance between A and B at t ¼ 30 min and the velocity of B relative to A.
        // {Ans.: dr ¼ 10.9 km, dv ¼ 10.8 m/s}
    }
}