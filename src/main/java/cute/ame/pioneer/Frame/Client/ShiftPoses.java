package cute.ame.pioneer.Frame.Client;

import cute.ame.pioneer.Core.Frame.FrameShift;
import dev.ryanhcode.sable.companion.math.Pose3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class ShiftPoses
{
    public static Pose3d apply(Pose3d pose, FrameShift shift, double[] scratch)
    {
        Vector3d p = pose.position();
        shift.position(p.x, p.y, p.z, scratch);
        p.set(scratch[0], scratch[1], scratch[2]);
        if (!shift.rotated) return pose;

        Quaterniond q = pose.orientation();
        shift.orientation(q.x, q.y, q.z, q.w, scratch);
        q.set(scratch[0], scratch[1], scratch[2], scratch[3]);
        return pose;
    }

    public static void store(FrameShift shift, double[] slots, int slot)
    {
        int i = slot * 7;
        slots[i] = shift.tx;
        slots[i + 1] = shift.ty;
        slots[i + 2] = shift.tz;
        slots[i + 3] = shift.rotated ? shift.qx : 0.0;
        slots[i + 4] = shift.rotated ? shift.qy : 0.0;
        slots[i + 5] = shift.rotated ? shift.qz : 0.0;
        slots[i + 6] = shift.rotated ? shift.qw : 1.0;
    }

    public static FrameShift load(double[] slots, int slot, FrameShift out)
    {
        int i = slot * 7;
        out.translation(slots[i], slots[i + 1], slots[i + 2], 0.0, 0.0, 0.0);
        if (slots[i + 3] == 0.0 && slots[i + 4] == 0.0 && slots[i + 5] == 0.0) return out;

        out.qx = slots[i + 3];
        out.qy = slots[i + 4];
        out.qz = slots[i + 5];
        out.qw = slots[i + 6];
        out.rotated = true;
        return out;
    }
}
