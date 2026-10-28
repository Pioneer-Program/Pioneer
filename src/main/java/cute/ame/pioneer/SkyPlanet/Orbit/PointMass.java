package cute.ame.pioneer.SkyPlanet.Orbit;

import org.joml.Vector3d;

public interface PointMass {
    public Double getM(); // mass, kg
    public Double getG() // universal gravitational parameter
    public Double getMu() // standard gravitational parameter, m^3/s^2
    public Vector3d getR(); // position, m
    public Vector3d getV(); // velocity, m/s
    public Vector3d getA(); // acceleration, m/s/s
    public String getId(); // unique identifier
    public Boolean getFix(); // flag, if true then mass will not be moved by physics

    public void setM(Double m); // mass, kg
    public void setG(Double G) // universal gravitational parameter
    public void setMu(Double mu) // standard gravitational parameter, m^3/s^2
    public void setR(Vector3d r); // position, m
    public void setV(Vector3d v); // velocity, m/s
    public void setA(Vector3d a); // acceleration, m/s/s
    public void setId(String id); // unique identifier
    public void setFix(Boolean fix); // flag, if true then mass will not be moved by physics
}
