package cute.ame.pioneer.Mixin.Sable;

import cute.ame.pioneer.Core.Frame.FrameShift;
import cute.ame.pioneer.Frame.Client.ShiftPoses;
import cute.ame.pioneer.Frame.Client.ShiftableInterpolator;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This lil class moves the ship on the client w/out interpolating across the jump, sable has no snap :c
// so we have to do it ourselves.. :dead:
@Mixin(value = SubLevelSnapshotInterpolator.class, remap = false)
public abstract class SnapshotInterpolatorShiftMixin implements ShiftableInterpolator
{
    @Unique private static final int PIONEER$SHIFTS = 4;

    @Unique private final int[] pioneer$tick = new int[PIONEER$SHIFTS];
    @Unique private final double[] pioneer$shifts = new double[PIONEER$SHIFTS * 7];
    @Unique private final double[] pioneer$scratch = new double[4];

    @Shadow @Final public ObjectArrayList<SubLevelSnapshotInterpolator.Snapshot> buffer;

    @Shadow @Final private Pose3d runningSnapshot;
    @Unique private int pioneer$count;
    @Unique private int pioneer$next;
    @Unique private boolean pioneer$replaying;

    @Shadow
    public abstract void receiveSnapshot(int gameTick, Pose3dc data);

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    // tell idea to shut up basically, dw, it's fine, buffer is final >:3
    public void pioneer$shift(int tick, FrameShift shift)
    {
        synchronized (buffer)
        {
            for (int i = 0, n = buffer.size(); i < n; ++i)
            {
                SubLevelSnapshotInterpolator.Snapshot s = buffer.get(i);
                if (s.gameTick() >= tick) continue;

                buffer.set(i, new SubLevelSnapshotInterpolator.Snapshot(s.gameTick(), ShiftPoses.apply(new Pose3d(s.pose()), shift, pioneer$scratch)));
            }
        }

        ShiftPoses.apply(runningSnapshot, shift, pioneer$scratch);
        int slot = pioneer$next;
        pioneer$tick[slot] = tick;
        ShiftPoses.store(shift, pioneer$shifts, slot);
        pioneer$next = (slot + 1) % PIONEER$SHIFTS;
        if (pioneer$count < PIONEER$SHIFTS) pioneer$count++;
    }

    @Inject(method = "receiveSnapshot", at = @At("HEAD"), cancellable = true)
    private void pioneer$shiftLateSnapshot(int gameTick, Pose3dc data, CallbackInfo ci)
    {
        if (pioneer$replaying || pioneer$count == 0) return;

        int start = pioneer$count < PIONEER$SHIFTS ? 0 : pioneer$next;
        Pose3d moved = null;
        FrameShift shift = null;
        double[] scratch = null;
        for (int k = 0; k < pioneer$count; ++k)
        {
            int slot = (start + k) % PIONEER$SHIFTS;
            if (gameTick >= pioneer$tick[slot]) continue;

            if (moved == null)
            {
                moved = new Pose3d(data);
                shift = new FrameShift();
                scratch = new double[4];
            }

            ShiftPoses.apply(moved, ShiftPoses.load(pioneer$shifts, slot, shift), scratch);
        }

        if (moved == null) return;

        ci.cancel();
        pioneer$replaying = true;
        try
        {
            receiveSnapshot(gameTick, moved);
        }
        finally
        {
            pioneer$replaying = false;
        }
    }
}
