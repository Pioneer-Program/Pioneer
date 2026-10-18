package cute.ame.pioneer.Item.Client.Preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cute.ame.pioneer.Item.Client.Name.NameClient;
import cute.ame.pioneer.Item.Name.NameServer;
import cute.ame.pioneer.Item.Name.ToolBoxColors;
import cute.ame.pioneer.Pioneer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

public final class NamePreview extends ToolBoxPreview
{
    private static final long NONE = Long.MIN_VALUE;
    private static final float NEAR = 2.0f;
    private static final float FAR = 4.0f;
    private static final float REACH = 32.0f;
    private static final float PLATE_DEPTH = 0.5f;
    private static final ResourceLocation PLATE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "textures/gui/toolbox/name_plate.png");
    private static final float TEXTURE_WIDTH = 24.0f;
    private static final float TEXTURE_HEIGHT = 16.0f;
    private static final float SLICE = 3.0f;
    private static final float PAD = 4.0f;
    private static final float ICON = 12.0f;
    private static final float ICON_GAP = 3.0f;
    private static final float POINTER = 3.0f;
    private static final float TOP = -POINTER - ICON - 2.0f * PAD;

    private final Reference2ObjectOpenHashMap<Block, ItemStack> stacks = new Reference2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final Vector3f axis = new Vector3f();
    private final Vector3d anchor = new Vector3d();

    private int count;
    private int aimedIndex;
    private float[] xs = new float[16];
    private float[] ys = new float[16];
    private float[] zs = new float[16];
    private float[] scales = new float[16];
    private float[] alphas = new float[16];
    private int[] colors = new int[16];
    private NameClient.Label[] labels = new NameClient.Label[16];
    private ItemStack[] icons = new ItemStack[16];

    private long lastFrame;
    private long focusPos = NONE;
    private float focus;
    private long fadingPos = NONE;
    private float fading;

    private static void plate(VertexConsumer consumer, Matrix4f matrix, float content, int argb, float z)
    {
        float left = -content / 2.0f - PAD;
        float right = content / 2.0f + PAD;
        for (int column = 0; column < 3; column++)
            for (int row = 0; row < 3; row++)
                quad(consumer, matrix, edge(column, left, right), edge(row, TOP, -POINTER), edge(column + 1, left, right), edge(row + 1, TOP, -POINTER), z, edge(column, 0.0f, 16.0f), edge(row, 0.0f, 16.0f), edge(column + 1, 0.0f, 16.0f), edge(row + 1, 0.0f, 16.0f), argb);

        quad(consumer, matrix, -2.5f, -POINTER, 2.5f, 0.0f, z, 16.0f, 0.0f, 21.0f, POINTER, argb);
    }

    private static float edge(int index, float start, float end)
    {
        return index == 0 ? start : index == 1 ? start + SLICE : index == 2 ? end - SLICE : end;
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1, float z, float u0, float v0, float u1, float v1, int argb)
    {
        u0 /= TEXTURE_WIDTH;
        v0 /= TEXTURE_HEIGHT;
        u1 /= TEXTURE_WIDTH;
        v1 /= TEXTURE_HEIGHT;
        consumer.addVertex(matrix, x0, y0, z).setColor(argb).setUv(u0, v0).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, x0, y1, z).setColor(argb).setUv(u0, v1).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, x1, y1, z).setColor(argb).setUv(u1, v1).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, x1, y0, z).setColor(argb).setUv(u1, v0).setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public void render(PreviewFrame frame)
    {
        long now = frame.now;
        if (now - lastFrame > 500L) NameClient.forget();

        float blend = 1.0f - (float) Math.exp(-Math.min(now - lastFrame, 100L) / 60.0f);
        lastFrame = now;

        if (NameClient.needsRequest(frame.player, 12.0)) NameClient.request(frame.player);

        long aimed = frame.hit == null ? NONE : frame.hit.getBlockPos().asLong();
        if (aimed != NONE && !NameServer.nameable(frame.level.getBlockEntity(cursor.set(aimed)))) aimed = NONE;

        if (aimed != focusPos)
        {
            fadingPos = focusPos;
            fading = focus;
            focusPos = aimed;
            focus = 0.0f;
        }
        focus += (1.0f - focus) * blend;
        fading -= fading * blend;
        frame.flush();

        collect(frame);
        if (count == 0) return;

        Camera camera = frame.minecraft.gameRenderer.getMainCamera();
        MultiBufferSource.BufferSource buffers = frame.minecraft.renderBuffers().bufferSource();
        Font font = frame.minecraft.font;
        float behind = camera.rotation().transform(0.0f, 0.0f, 1.0f, axis).dot(camera.getLookVector()) > 0.0f ? PLATE_DEPTH : -PLATE_DEPTH;

        layer(frame, camera, buffers, font, true, 0.25f, behind, -1);
        layer(frame, camera, buffers, font, false, 1.0f, behind, -1);
        if (aimedIndex < 0) return;

        GL11.glDepthRange(0.0, 0.01);
        layer(frame, camera, buffers, font, false, 1.0f, behind, aimedIndex);
        GL11.glDepthRange(0.0, 1.0);
    }

    private void collect(PreviewFrame frame)
    {
        count = 0;
        aimedIndex = -1;
        for (Long2ObjectMap.Entry<NameClient.Label> entry : Long2ObjectMaps.fastIterable(NameClient.labels()))
        {
            long pos = entry.getLongKey();
            cursor.set(pos);
            if (!NameServer.nameable(frame.level.getBlockEntity(cursor))) continue;

            float emphasis = pos == focusPos ? focus : pos == fadingPos ? fading : 0.0f;
            BlockState state = frame.level.getBlockState(cursor);
            VoxelShape shape = state.getShape(frame.level, cursor);
            double top = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);

            frame.toWorld(cursor.getX() + 0.5, cursor.getY() + top + 0.15 + 0.1 * emphasis, cursor.getZ() + 0.5, anchor);
            float dx = (float) (anchor.x - frame.camera.x);
            float dy = (float) (anchor.y - frame.camera.y);
            float dz = (float) (anchor.z - frame.camera.z);
            float distance = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            float alpha = Mth.clamp((REACH - distance) / 8.0f, 0.0f, 1.0f);
            if (alpha < 0.03f) continue;

            if (count == xs.length) grow();
            xs[count] = dx;
            ys[count] = dy;
            zs[count] = dz;
            scales[count] = 0.0075f * Mth.clamp(distance, NEAR, FAR) * Mth.lerp(emphasis, 1.0f, 1.25f);
            alphas[count] = alpha;
            colors[count] = ToolBoxColors.colorOf(state);
            labels[count] = entry.getValue();
            icons[count] = icon(state.getBlock());
            if (pos == focusPos) aimedIndex = count;
            count++;
        }
    }

    private void layer(PreviewFrame frame, Camera camera, MultiBufferSource.BufferSource buffers, Font font, boolean seeThrough, float fade, float behind, int only)
    {
        PoseStack pose = frame.pose;
        int from = only < 0 ? 0 : only;
        int to = only < 0 ? count : only + 1;
        int skip = only < 0 && !seeThrough ? aimedIndex : -1;

        VertexConsumer plates = buffers.getBuffer(seeThrough ? RenderType.textSeeThrough(PLATE) : RenderType.text(PLATE));
        for (int i = from; i < to; i++)
        {
            int a = Math.round(alphas[i] * fade * 255.0f);
            if (a < 8 || i == skip) continue;

            place(pose, camera, i);
            plate(plates, pose.last().pose(), content(i), a << 24 | colors[i], behind);
            pose.popPose();
        }
        buffers.endBatch();

        Font.DisplayMode mode = seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
        for (int i = from; i < to; i++)
        {
            int a = Math.round(alphas[i] * fade * 255.0f);
            if (a < 8 || i == skip) continue;

            place(pose, camera, i);
            float left = -content(i) / 2.0f;
            ItemStack icon = icons[i];
            if (!icon.isEmpty())
            {
                if (!seeThrough && a >= 128)
                {
                    pose.pushPose();
                    pose.translate(left + ICON / 2.0f, TOP + PAD + ICON / 2.0f, -behind * 2.0f);
                    pose.scale(ICON, -ICON, 1.0f);
                    frame.minecraft.getItemRenderer().renderStatic(icon, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, frame.level, 0);
                    pose.popPose();
                }
                left += ICON + ICON_GAP;
            }

            font.drawInBatch(labels[i].line(), left, TOP + PAD + 2.0f, a << 24 | colors[i], false, pose.last().pose(), buffers, mode, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        buffers.endBatch();
    }

    private float content(int i)
    {
        return labels[i].width() + (icons[i].isEmpty() ? 0.0f : ICON + ICON_GAP);
    }

    private ItemStack icon(Block block)
    {
        ItemStack stack = stacks.get(block);
        if (stack == null)
        {
            stack = new ItemStack(block);
            stacks.put(block, stack);
        }
        return stack;
    }

    private void place(PoseStack pose, Camera camera, int i)
    {
        float scale = scales[i];
        pose.pushPose();
        pose.translate(xs[i], ys[i], zs[i]);
        pose.mulPose(camera.rotation());
        pose.scale(scale, -scale, scale);
    }

    private void grow()
    {
        int size = xs.length * 2;
        xs = Arrays.copyOf(xs, size);
        ys = Arrays.copyOf(ys, size);
        zs = Arrays.copyOf(zs, size);
        scales = Arrays.copyOf(scales, size);
        alphas = Arrays.copyOf(alphas, size);
        colors = Arrays.copyOf(colors, size);
        labels = Arrays.copyOf(labels, size);
        icons = Arrays.copyOf(icons, size);
    }
}
