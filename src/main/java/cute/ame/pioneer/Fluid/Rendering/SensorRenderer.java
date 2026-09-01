package cute.ame.pioneer.Fluid.Rendering;

import cute.ame.pioneer.Fluid.Block.SensorBlock;
import cute.ame.pioneer.Fluid.BlockEntity.SensorBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import cute.ame.pioneer.Pioneer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.jetbrains.annotations.NotNull;

public class SensorRenderer implements BlockEntityRenderer<SensorBlockEntity>
{
    private static final float REST_DEGREES = 0.0f;
    private static final float SWEEP_DEGREES = -90.0f;

    // use the texture of thermometer, but can be swapped, ask @rv_ndm, i implemented that like that, sorry.
    public static final ModelResourceLocation NEEDLE = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "block/sensor_needle"));

    public SensorRenderer(BlockEntityRendererProvider.Context context)
    {

    }

    @Override
    public void render(@NotNull SensorBlockEntity sensor, float partialTick, @NotNull PoseStack pose, @NotNull MultiBufferSource buffers, int light, int overlay)
    {
        BlockState state = sensor.getBlockState();
        if (!(state.getBlock() instanceof SensorBlock block)) return;

        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(NEEDLE);

        pose.pushPose();
        orient(pose, state);

        pose.translate(0.3f, 0.3f, 0.3f);
        pose.mulPose(Axis.YP.rotationDegrees(sensor.dial() * (REST_DEGREES + SWEEP_DEGREES)));
        pose.translate(-0.3f, -0.3f, -0.3f);
        pose.translate(0f, -0.05f, -0.2f);

        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, state, model, 1.0f, 1.0f, 1.0f, light, overlay);

        pose.popPose();
    }

    private static void orient(PoseStack pose, BlockState state)
    {
        AttachFace face = state.getValue(SensorBlock.FACE);
        Direction facing = state.getValue(SensorBlock.FACING);

        int x;
        int y;

        switch (face)
        {
            case FLOOR ->
            {
                x = 0;
                y = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
            }
            case WALL ->
            {
                x = 90;
                y = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
            }
            default ->
            {
                x = 180;
                y = switch (facing) { case EAST -> 270; case SOUTH -> 0; case WEST -> 90; default -> 180; };
            }
        }

        if (x == 0 && y == 0) return;

        pose.translate(0.5f, 0.5f, 0.5f);
        if (y != 0) pose.mulPose(Axis.YP.rotationDegrees(-y));
        if (x != 0) pose.mulPose(Axis.XP.rotationDegrees(-x));
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    @Override
    public int getViewDistance()
    {
        return 32;
    }
}
