package cute.ame.pioneer.Mixin.Sable;

import cute.ame.pioneer.Core.Frame.FrameShift;
import cute.ame.pioneer.Frame.Client.ShiftPoses;
import cute.ame.pioneer.Frame.Client.ShiftableInterpolator;
import cute.ame.pioneer.Frame.Client.ShiftableSubLevel;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ClientSubLevel.class, remap = false)
public abstract class ClientSubLevelShiftMixin implements ShiftableSubLevel
{
    @Unique private final double[] pioneer$scratch = new double[4];

    @Shadow
    @Final
    private SubLevelSnapshotInterpolator interpolator;
    @Shadow
    private float lastRenderPosePartialTick;

    @Override
    public void pioneer$shift(int tick, FrameShift shift)
    {
        ClientSubLevel self = (ClientSubLevel) (Object) this;
        ShiftPoses.apply(self.logicalPose(), shift, pioneer$scratch);
        ShiftPoses.apply((Pose3d) self.lastPose(), shift, pioneer$scratch);
        ((ShiftableInterpolator) interpolator).pioneer$shift(tick, shift);
        lastRenderPosePartialTick = -1.0f;
        self.forceUpdateBounds();
    }
}
