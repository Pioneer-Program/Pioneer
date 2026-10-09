package cute.ame.pioneer.Item.Client.Preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

    public VertexConsumer ghosts()
    {
        if (ghosts == null)
            ghosts = minecraft.renderBuffers().bufferSource().getBuffer(Sheets.translucentCullBlockSheet());

        return ghosts;
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
