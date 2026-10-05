package cute.ame.pioneer.Mixin.Sable;

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
    @Unique private final double[] pioneer$delta = new double[PIONEER$SHIFTS * 3];

    @Shadow @Final public ObjectArrayList<SubLevelSnapshotInterpolator.Snapshot> buffer;

    @Shadow @Final private Pose3d runningSnapshot;
    @Unique private int pioneer$count;
    @Unique private int pioneer$next;
    @Unique private boolean pioneer$replaying;

    @Unique
    private static Pose3d pioneer$moved(Pose3dc pose, double dx, double dy, double dz)
    {
        Pose3d moved = new Pose3d(pose);
        moved.position().add(dx, dy, dz);
        return moved;
    }

    @Shadow
    public abstract void receiveSnapshot(int gameTick, Pose3dc data);

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    // tell idea to shut up basically, dw, it's fine, buffer is final >:3
    public void pioneer$shift(int tick, double dx, double dy, double dz)
    {
        synchronized (buffer)
        {
            for (int i = 0, n = buffer.size(); i < n; i++)
            {
                SubLevelSnapshotInterpolator.Snapshot s = buffer.get(i);
                if (s.gameTick() < tick)
                    buffer.set(i, new SubLevelSnapshotInterpolator.Snapshot(s.gameTick(), pioneer$moved(s.pose(), dx, dy, dz)));
            }
        }

        runningSnapshot.position().add(dx, dy, dz);
        int slot = pioneer$next;
        pioneer$tick[slot] = tick;
        pioneer$delta[slot * 3] = dx;
        pioneer$delta[slot * 3 + 1] = dy;
        pioneer$delta[slot * 3 + 2] = dz;
        pioneer$next = (slot + 1) % PIONEER$SHIFTS;
        if (pioneer$count < PIONEER$SHIFTS) pioneer$count++;
    }

    @Inject(method = "receiveSnapshot", at = @At("HEAD"), cancellable = true)
    private void pioneer$shiftLateSnapshot(int gameTick, Pose3dc data, CallbackInfo ci)
    {
        if (pioneer$replaying || pioneer$count == 0) return;

        double dx = 0.0, dy = 0.0, dz = 0.0;
        boolean late = false;
        for (int i = 0; i < pioneer$count; i++)
        {
            if (gameTick >= pioneer$tick[i]) continue;
            dx += pioneer$delta[i * 3];
            dy += pioneer$delta[i * 3 + 1];
            dz += pioneer$delta[i * 3 + 2];
            late = true;
        }
        if (!late) return;

        ci.cancel();
        pioneer$replaying = true;
        try
        {
            receiveSnapshot(gameTick, pioneer$moved(data, dx, dy, dz));
        }
        finally
        {
            pioneer$replaying = false;
        }
    }
}
