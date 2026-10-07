package cute.ame.pioneer.Item.Client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.MultiToolMode;
import cute.ame.pioneer.Item.MultiToolRotation;
import cute.ame.pioneer.Pioneer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.List;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class MultiToolRotationPreview
{
    private static final Direction[] SIDES = Direction.values();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Quaternionf TURN = new Quaternionf();

    private static final long APPEAR_MS = 200L;
    private static final long TURN_MS = 600L;
    private static final long FADE_MS = 300L;
    private static final long CYCLE_MS = APPEAR_MS + TURN_MS + 900L + FADE_MS;
    private static final float OVERSHOOT = 1.1f;

    private static long shownPos;
    private static BlockState shownFrom;
    private static BlockState shownTo;
    private static long shownAt;
    private static long lastFrame;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.options.hideGui) return;

        ItemStack stack = MultiToolItem.held(player);
        if (stack.isEmpty() || MultiToolItem.modeOf(stack) != MultiToolMode.ROTATE) return;

        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(MultiToolRotation.ROTATABLE)) return;

        Direction face = hit.getDirection();
        boolean reversed = player.isSecondaryUseActive();
        BlockState next = MultiToolRotation.turn(state, face, reversed);
        if (!(next != state && next.canSurvive(level, pos) && !MultiToolRotation.movesMouth(pos, state, next))) return;

        long now = Util.getMillis();
        Vec3 camera = event.getCamera().getPosition();
        RenderType type = Sheets.translucentCullBlockSheet();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(type);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.scale(0.99f, 0.99f, 0.99f);
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
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
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(TURN.rotationAxis((eased - 1.0f) * Mth.HALF_PI, sign * face.getStepX(), sign * face.getStepY(), sign * face.getStepZ()));
        pose.translate(-0.5f, -0.5f, -0.5f);
        draw(consumer, pose.last(), minecraft.getBlockRenderer().getBlockModel(next), next, 0.55f, 0.85f, 1.0f, alpha);

        pose.popPose();
        GL11.glDepthRange(0.0, 0.01);
        buffers.endBatch(type);
        GL11.glDepthRange(0.0, 1.0);
    }

    private static void draw(VertexConsumer consumer, PoseStack.Pose pose, BakedModel model, BlockState state, float red, float green, float blue, float alpha)
    {
        for (Direction side : SIDES) emit(consumer, pose, model, state, side, red, green, blue, alpha);
        emit(consumer, pose, model, state, null, red, green, blue, alpha);
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, BakedModel model, BlockState state, @Nullable Direction side, float red, float green, float blue, float alpha)
    {
        RANDOM.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(state, side, RANDOM, ModelData.EMPTY, null);

        for (int i = 0, n = quads.size(); i < n; i++)
            consumer.putBulkData(pose, quads.get(i), red, green, blue, alpha, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
