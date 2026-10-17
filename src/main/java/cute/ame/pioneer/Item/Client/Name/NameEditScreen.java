package cute.ame.pioneer.Item.Client.Name;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Item.Name.NameServer;
import cute.ame.pioneer.Item.Network.NameSetPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

// TODO: i should maybe just use the nameplate directly, would be cool imo
public class NameEditScreen extends Screen
{
    private static final Component NAME_HINT = Component.translatable("name.pioneer.hint");
    private static final Component EMPTY = Component.translatable("name.pioneer.empty");

    private static final int ICON = 16;
    private static final float CENTER_SCALE = 1.5f;

    private final long pos;
    private final String original;
    private final ItemStack icon;
    private String text;
    private int centerX;
    private int centerY;
    private long openedAt;

    public NameEditScreen(long pos, String current, ItemStack icon)
    {
        super(Component.translatable("multitool.pioneer.mode.name"));
        this.pos = pos;
        this.original = current;
        this.text = NameServer.clean(current);
        this.icon = icon;
    }

    @Override
    protected void init()
    {
        centerX = width / 2;
        centerY = height / 2 - 46;
        openedAt = Util.getMillis();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        long now = Util.getMillis();
        float appear = Mth.clamp((now - openedAt) / 150.0f, 0.0f, 1.0f);
        appear = 1.0f - (1.0f - appear) * (1.0f - appear) * (1.0f - appear);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0f);
        pose.scale(appear, appear, 1.0f);

        pose.pushPose();
        pose.scale(CENTER_SCALE, CENTER_SCALE, 1.0f);
        graphics.renderFakeItem(icon, -ICON / 2, -ICON / 2);
        pose.popPose();

        int textY = -Math.round(ICON * CENTER_SCALE / 2.0f) - 4 - font.lineHeight;
        boolean caret = (now - openedAt) % 1000L < 500L;

        if (text.isEmpty())
        {
            graphics.drawCenteredString(font, EMPTY, 0, textY, 0x777777);
            if (caret)
                graphics.fill(-font.width(EMPTY) / 2 - 2, textY - 1, -font.width(EMPTY) / 2 - 1, textY + font.lineHeight, 0xFFFFFFFF);
        }
        else
        {
            int left = -font.width(text) / 2;
            graphics.drawString(font, text, left, textY, 0xFFFFFF, true);
            if (caret)
                graphics.fill(left + font.width(text) + 1, textY - 1, left + font.width(text) + 2, textY + font.lineHeight, 0xFFFFFFFF);
        }

        int nameY = Math.round(ICON * CENTER_SCALE / 2.0f) + 4;
        graphics.drawCenteredString(font, title, 0, nameY, 0xAAAAAA);
        graphics.drawCenteredString(font, NAME_HINT, 0, nameY + font.lineHeight + 3, 0x777777);

        pose.popPose();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        boolean letter = NameServer.letter(codePoint);
        if (text.length() >= NameServer.MAX_LENGTH) return false;
        if (!letter && !NameServer.digit(codePoint))
        {
            if (codePoint != ' ' && codePoint != '_' && codePoint != '-' && codePoint != '.') return false;
            if (!text.isEmpty() && !text.endsWith("_")) text += '_';
            return true;
        }

        if (text.isEmpty() && !letter) return false;

        text += Character.toLowerCase(codePoint);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        {
            confirm();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE)
        {
            if (!text.isEmpty()) text = Screen.hasControlDown() ? "" : text.substring(0, text.length() - 1);
            return true;
        }

        if (Screen.isPaste(keyCode))
        {
            assert minecraft != null;
            text = NameServer.clean(text + minecraft.keyboardHandler.getClipboard());
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm()
    {
        String label = NameServer.clean(text);
        if (!label.equals(original)) PacketDistributor.sendToServer(new NameSetPayload(pos, label));
        onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
