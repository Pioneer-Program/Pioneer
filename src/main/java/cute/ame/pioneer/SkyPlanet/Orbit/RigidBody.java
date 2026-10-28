package cute.ame.pioneer.SkyPlanet.Orbit;

import org.joml.Vector3d;
import org.joml.Matrix3d;
import org.joml.Quaterniond;

public interface RigidBody extends PointMass {
    public Quaterniond qBodyRef(); // orientation of body-fixed frame relative to an arbitrary reference frame
    public Vector3d getOmega(); // angular velocity, rad/s
    public Vector3d getAlpha(); // angular acceleration, rad/s/s
    public Vector3d getCom(); // center of mass location relative to coordinate origin, m
    public Matrix3d getI(); // inertia matrix in body-fixed frame with origin at center of mass, kg*m^2
}
