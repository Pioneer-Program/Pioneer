package cute.ame.pioneer.Mixin.Sable;

import cute.ame.pioneer.Frame.Client.ShiftableInterpolator;
import cute.ame.pioneer.Frame.Client.ShiftableSubLevel;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClientSubLevel.class, remap = false)
public abstract class ClientSubLevelShiftMixin implements ShiftableSubLevel
{
    @Shadow
    @Final
    private SubLevelSnapshotInterpolator interpolator;
    @Shadow
    private float lastRenderPosePartialTick;

    @Override
    public void pioneer$shift(int tick, double dx, double dy, double dz)
    {
        ClientSubLevel self = (ClientSubLevel) (Object) this;
        self.logicalPose().position().add(dx, dy, dz);
        ((Pose3d) self.lastPose()).position().add(dx, dy, dz);
        ((ShiftableInterpolator) interpolator).pioneer$shift(tick, dx, dy, dz);
        lastRenderPosePartialTick = -1.0f;
        self.forceUpdateBounds();
    }
}
