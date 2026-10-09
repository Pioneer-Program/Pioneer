package cute.ame.pioneer.Item.Client.Preview;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Item.Module.RotateModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Quaternionf;

public final class RotatePreview extends MultiToolPreview
{
    private static final long APPEAR_MS = 200L;
    private static final long TURN_MS = 600L;
    private static final long FADE_MS = 300L;
    private static final long CYCLE_MS = APPEAR_MS + TURN_MS + 900L + FADE_MS;
    private static final float OVERSHOOT = 1.1f;

    private final Quaternionf turn = new Quaternionf();

    private long shownPos;
    private BlockState shownFrom;
    private BlockState shownTo;
    private long shownAt;
    private long lastFrame;

    @Override
    public void render(PreviewFrame frame)
    {
        BlockHitResult hit = frame.hit;
        if (hit == null) return;

        BlockPos pos = hit.getBlockPos();
        BlockState state = frame.level.getBlockState(pos);
        if (!state.is(RotateModule.ROTATABLE)) return;

        Direction face = hit.getDirection();
        boolean reversed = frame.player.isSecondaryUseActive();
        BlockState next = RotateModule.turn(state, face, reversed);
        if (!(next != state && next.canSurvive(frame.level, pos) && !RotateModule.movesMouth(pos, state, next))) return;

        long now = frame.now;
        long packed = pos.asLong();
        if (packed != shownPos || state != shownFrom || next != shownTo || now - lastFrame > 250L)
        {
            shownPos = packed;
            shownFrom = state;
            shownTo = next;
            shownAt = now;
        }

        lastFrame = now;
        long elapsed = (now - shownAt) % CYCLE_MS;
        float back = Mth.clamp((elapsed - APPEAR_MS) / (float) TURN_MS, 0.0f, 1.0f) - 1.0f;
        float eased = 1.0f + (OVERSHOOT + 1.0f) * back * back * back + OVERSHOOT * back * back;
        float alpha = 0.55f * Math.min(Mth.clamp(elapsed / (float) APPEAR_MS, 0.0f, 1.0f), Mth.clamp((CYCLE_MS - elapsed) / (float) FADE_MS, 0.0f, 1.0f));
        float sign = reversed ? 1.0f : -1.0f;

        PoseStack pose = frame.pose;
        pose.pushPose();
        pose.scale(0.99f, 0.99f, 0.99f);
        ghost(frame, pos, next, turn.rotationAxis((eased - 1.0f) * Mth.HALF_PI, sign * face.getStepX(), sign * face.getStepY(), sign * face.getStepZ()), 0.55f, 0.85f, 1.0f, alpha);
        pose.popPose();
    }
}
