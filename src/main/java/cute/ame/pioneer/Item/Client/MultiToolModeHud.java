package cute.ame.pioneer.Item.Client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.MultiToolMode;
import cute.ame.pioneer.Item.Network.MultiToolModePayload;
import cute.ame.pioneer.Pioneer;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class MultiToolModeHud
{
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_mode");

    private static final int ICON = 16;
    private static final int TEXTURE = 32;
    private static final int REACH = 2;

    private static final float CENTER_SCALE = 1.5f;
    private static final long FADE_MS = 450L;

    private static float slideFrom;
    private static long slideStart;
    private static long appearedAt;
    private static long visibleUntil;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER, MultiToolModeHud::render);
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event)
    {
        if (!Screen.hasAltDown()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        ItemStack stack = MultiToolItem.held(player);
        if (stack.isEmpty()) return;

        double delta = event.getScrollDeltaY();
        if (delta == 0.0) return;

        boolean forward = delta < 0.0;
        long now = Util.getMillis();

        MultiToolMode next = MultiToolItem.modeOf(stack).cycle(forward);
        MultiToolItem.setMode(stack, next);
        PacketDistributor.sendToServer(new MultiToolModePayload(forward));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), pitch(next), 0.35f));

        slideFrom = Mth.clamp(offset(now) + (forward ? 1.0f : -1.0f), -REACH, REACH);
        slideStart = now;
        show(now);

        event.setCanceled(true);
    }

    private static float pitch(MultiToolMode mode)
    {
        return (float) Math.pow(2.0, mode.ordinal() * 4.0f / 12.0);
    }

    private static void show(long now)
    {
        if (now >= visibleUntil) appearedAt = now;
        visibleUntil = now + 900L + FADE_MS;
    }

    private static float offset(long now)
    {
        float t = Mth.clamp((now - slideStart) / (float) 170L, 0.0f, 1.0f);
        float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);

        return slideFrom * (1.0f - eased);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;

        ItemStack stack = MultiToolItem.held(player);
        if (stack.isEmpty()) return;

        long now = Util.getMillis();
        if (Screen.hasAltDown()) show(now);

        float alpha = Math.min(Mth.clamp((now - appearedAt) / (float) 220L, 0.0f, 1.0f), Mth.clamp((visibleUntil - now) / (float) FADE_MS, 0.0f, 1.0f));
        if (alpha <= 0.0f) return;

        int reach = Math.min(1, MultiToolMode.count() - 1);
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() - 96;
        float offset = offset(now);
        MultiToolMode current = MultiToolItem.modeOf(stack);
        RenderSystem.enableBlend();

        PoseStack pose = graphics.pose();
        float fadeEdge = reach + 0.5f;

        for (int k = -reach - 1; k <= reach + 1; k++)
        {
            float position = k + offset;
            float distance = Math.abs(position);
            if (distance >= fadeEdge) continue;

            float focus = 1.0f - Math.min(distance, 1.0f);
            float scale = Mth.lerp(focus, 1.0f, CENTER_SCALE);
            float tint = alpha * Mth.lerp(focus, 0.38f, 1.0f) * Math.min((fadeEdge - distance) * 2.0f, 1.0f);

            pose.pushPose();
            pose.translate(centerX + position * 30, centerY, 0.0f);
            pose.scale(scale, scale, 1.0f);
            graphics.setColor(1.0f, 1.0f, 1.0f, tint);
            graphics.blit(MultiToolMode.byIndex(current.ordinal() + k).icon(), -ICON / 2, -ICON / 2, ICON, ICON, 0.0f, 0.0f, TEXTURE, TEXTURE, TEXTURE, TEXTURE);
            pose.popPose();
        }

        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        int text = Math.round(alpha * 255.0f);
        if (text < 8) return;

        Font font = minecraft.font;
        Component title = current.title();
        graphics.drawString(font, title, centerX - font.width(title) / 2, centerY + Math.round(ICON * CENTER_SCALE / 2.0f) + 4, (text << 24) | 0xFFFFFF, true);
    }
}
