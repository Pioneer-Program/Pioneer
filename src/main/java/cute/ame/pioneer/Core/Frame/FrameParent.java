package cute.ame.pioneer.Core.Frame;

import org.joml.Quaterniond;
import org.joml.Vector3d;

public interface FrameParent
{
    String name();

    Vector3d positionAt(long tick, double partial, Vector3d dest);

    Vector3d velocityAt(long tick, double partial, Vector3d dest);

    Quaterniond orientationAt(long tick, double partial, Quaterniond dest);

    Vector3d spinAt(long tick, Vector3d dest);
}
