package cute.ame.pioneer.Item.Client.Preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.opengl.GL11;

public final class PreviewFrame
{
    public Minecraft minecraft;
    public ClientLevel level;
    public LocalPlayer player;
    public ItemStack stack;
    public PoseStack pose;
    public Vec3 camera;
    public long now;
    public @Nullable BlockHitResult hit;
    private VertexConsumer ghosts;
    private final Quaternionf orientation = new Quaternionf();

    public VertexConsumer ghosts()
    {
        if (ghosts == null)
            ghosts = minecraft.renderBuffers().bufferSource().getBuffer(Sheets.translucentCullBlockSheet());

        return ghosts;
    }

    public void place(PoseStack pose, double x, double y, double z)
    {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(x, z);
        if (subLevel == null)
        {
            pose.translate(x - camera.x, y - camera.y, z - camera.z);
            return;
        }

        Pose3dc render = subLevel.renderPose();
        Vector3dc origin = render.position();
        Vector3dc pivot = render.rotationPoint();
        Vector3dc scale = render.scale();
        pose.translate(origin.x() - camera.x, origin.y() - camera.y, origin.z() - camera.z);
        pose.mulPose(orientation.set(render.orientation()));
        pose.scale((float) scale.x(), (float) scale.y(), (float) scale.z());
        pose.translate(x - pivot.x(), y - pivot.y(), z - pivot.z());
    }

    public Vector3d toWorld(double x, double y, double z, Vector3d out)
    {
        out.set(x, y, z);
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(x, z);
        return subLevel == null ? out : subLevel.renderPose().transformPosition(out);
    }

    void flush()
    {
        if (ghosts == null) return;

        RenderType type = Sheets.translucentCullBlockSheet();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        GL11.glDepthRange(0.0, 0.01);
        buffers.endBatch(type);
        GL11.glDepthRange(0.0, 1.0);

        ghosts = null;
    }
}
