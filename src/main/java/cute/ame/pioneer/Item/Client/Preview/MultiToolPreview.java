package cute.ame.pioneer.Item.Client.Preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

public abstract class MultiToolPreview
{
    private static final Direction[] SIDES = Direction.values();
    private static final RandomSource RANDOM = RandomSource.create();

    protected static void ghost(PreviewFrame frame, BlockPos pos, BlockState state, @Nullable Quaternionf turn, float red, float green, float blue, float alpha)
    {
        PoseStack pose = frame.pose;
        pose.pushPose();
        pose.translate(pos.getX() - frame.camera.x, pos.getY() - frame.camera.y, pos.getZ() - frame.camera.z);

        if (turn != null)
        {
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(turn);
            pose.translate(-0.5f, -0.5f, -0.5f);
        }

        VertexConsumer consumer = frame.ghosts();
        BakedModel model = frame.minecraft.getBlockRenderer().getBlockModel(state);
        PoseStack.Pose last = pose.last();

        for (Direction side : SIDES) emit(consumer, last, model, state, side, red, green, blue, alpha);
        emit(consumer, last, model, state, null, red, green, blue, alpha);

        pose.popPose();
    }

    private static void emit(VertexConsumer consumer, PoseStack.Pose pose, BakedModel model, BlockState state, @Nullable Direction side, float red, float green, float blue, float alpha)
    {
        RANDOM.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(state, side, RANDOM, ModelData.EMPTY, null);

        for (int i = 0, n = quads.size(); i < n; i++)
            consumer.putBulkData(pose, quads.get(i), red, green, blue, alpha, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    public abstract void render(PreviewFrame frame);
}
