package cute.ame.pioneer.SkyPlanet.Orbit;

import org.joml.Vector3d;

public class MassObject implements PointMass
{
    Double mass; /* mass, kg */
    Double gravParam; /* universal gravitational parameter */
    Double mu; /* standard gravitational parameter, m^3/s^2 */

    Vector3d r; /* position, m */
    Vector3d v; /* velocity, m/s */
    Vector3d a; /* acceleration, m/s/s */

    String id; /* unique identifier */
    Boolean fix; /* flag: if true, no physics updates */

    public MassObject(Double m, Double G, Vector3d r, Vector3d v, Vector3d a, String id, Boolean fix)
    {
        this.mass = m;
        this.gravParam = G;
        this.mu = G * m;

        this.r = r;
        this.v = v;
        this.a = a;

        this.id = id;
        this.fix = fix;
    }

    public Double getM() // mass, kg
    {
        return this.mass;
    }

    public Double getG() // universal gravitational parameter
    {
        return this.mu;
    }

    public void setM(Double m) // mass, kg
    {
        this.mass = m;
        this.mu = this.gravParam * m;
    }

    public void setG(Double G) // universal gravitational parameter
    {
        this.gravParam = G;
        this.mu = G * this.mass;
    }

    /* PointMass interface */

    public Double getMu() // standard gravitational parameter, m^3/s^2
    {
        return this.mu;
    }

    public Vector3d getR() // position, m
    {
        return this.r;
    }

    public Vector3d getV() // velocity, m/s
    {
        return this.v;
    }

    public Vector3d getA() // acceleration, m/s/s
    {
        return this.a;
    }

    public String getId() // unique identifier
    {
        return this.id;
    }

    public Boolean getFix() // flag: if true, do not update via physics
    {
        return this.fix;
    }

    public void setMu(Double mu) // standard gravitational parameter, m^3/s^2
    {
        this.mu = mu;
        this.mass = mu / this.gravParam;
    }

    public void setR(Vector3d r) // position, m
    {
        this.r = r;
    }

    public void setV(Vector3d v) // velocity, m/s
    {
        this.v = v;
    }

    public void setA(Vector3d a) // acceleration, m/s/s
    {
        this.a = a;
    }

    public void setId(String id) // unique identifier
    {
        this.id = id;
    }

    public void setFix(Boolean fix) // flag: if true, do not update via physics
    {
        this.fix = fix;
    }

    /* Convenience */
    public Vector3d getRkm() // position, km
    {
        return this.r.mul(1.0e-3d);
    }

    public Vector3d getVkm() // velocity, km/s
    {
        return this.v.mul(1.0e-3d);
    }
}
