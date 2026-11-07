package cute.ame.pioneer.Item.Client.Tune;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Core.Configure.TuneOption;
import cute.ame.pioneer.Item.Network.TuneSetPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class TuneDialScreen extends Screen
{
    private static final Component MINUS = Component.literal("-");
    private static final Component PLUS = Component.literal("+");
    private static final Component DIAL_HINT = Component.translatable("tune.pioneer.dial_hint");

    private static final int ICON = 16;
    private static final int SPACING = 30;
    private static final float CENTER_SCALE = 1.5f;
    private static final float SIGN_SCALE = 1.25f;
    private static final float SIGN_PUSH_SCALE = SIGN_SCALE * 1.5f;
    private static final float DEAD_ZONE = 10.0f;

    private final long pos;
    private final int index;
    private final TuneOption option;
    private final ItemStack icon;
    private final int steps;
    private final float topRate;

    private int step;
    private int centerX;
    private int centerY;
    private double mouseX;
    private float carry;
    private float minusScale = SIGN_SCALE;
    private float plusScale = SIGN_SCALE;
    private float slideFrom;
    private long slideStart;
    private long openedAt;
    private long lastFrame;
    private long lastTick;

    public TuneDialScreen(long pos, int index, TuneOption option, ItemStack icon)
    {
        super(Component.translatable(option.key()));
        this.pos = pos;
        this.index = index;
        this.option = option;
        this.icon = icon;
        this.steps = option.steps();
        this.step = option.index(option.value());
        this.topRate = Math.max(12.0f, steps / 2.5f);
    }

    @Override
    protected void init()
    {
        centerX = width / 2;
        centerY = height / 2 - 46;
        mouseX = centerX;

        long now = Util.getMillis();
        openedAt = now;
        lastFrame = now;

        Window window = minecraft.getWindow();
        double scale = window.getGuiScale();
        GLFW.glfwSetCursorPos(window.getWindow(), centerX * scale, centerY * scale);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        long now = Util.getMillis();
        float seconds = Math.min((now - lastFrame) / 1000.0f, 0.1f);
        lastFrame = now;

        float offset = (float) (this.mouseX - centerX);
        float push = Mth.clamp((Math.abs(offset) - DEAD_ZONE) / (90.0f - DEAD_ZONE), 0.0f, 1.0f);
        int direction = push > 0.0f ? (offset > 0.0f ? 1 : -1) : 0;

        if (direction == 0) carry = 1.0f;
        else
        {
            carry += Mth.lerp(push * push, 2.0f, topRate) * seconds;
            while (carry >= 1.0f && move(step + direction)) carry -= 1.0f;
            if (carry > 1.0f) carry = 1.0f;
        }

        float appear = Mth.clamp((now - openedAt) / (float) 150L, 0.0f, 1.0f);
        appear = 1.0f - (1.0f - appear) * (1.0f - appear) * (1.0f - appear);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0f);
        pose.scale(appear, appear, 1.0f);

        float ease = 1.0f - (float) Math.exp(-seconds * 1000.0f / 80.0f);
        minusScale += (Mth.lerp(direction < 0 ? push : 0.0f, SIGN_SCALE, SIGN_PUSH_SCALE) - minusScale) * ease;
        plusScale += (Mth.lerp(direction > 0 ? push : 0.0f, SIGN_SCALE, SIGN_PUSH_SCALE) - plusScale) * ease;

        if (step > 0) sign(graphics, MINUS, -SPACING, minusScale);
        if (step < steps - 1) sign(graphics, PLUS, SPACING, plusScale);

        pose.pushPose();
        pose.scale(CENTER_SCALE, CENTER_SCALE, 1.0f);
        graphics.renderFakeItem(icon, -ICON / 2, -ICON / 2);
        pose.popPose();

        float slide = Mth.clamp((now - slideStart) / (float) 200L, 0.0f, 1.0f);
        slide = 1.0f - (1.0f - slide) * (1.0f - slide) * (1.0f - slide);
        Component value = option.label(option.valueAt(step));
        int valueX = Math.round(slideFrom * 8.0f * (1.0f - slide)) - font.width(value) / 2;
        int valueY = -Math.round(ICON * CENTER_SCALE / 2.0f) - 4 - font.lineHeight;
        graphics.drawString(font, value, valueX, valueY, 0xFFFFFF, true);

        int nameY = Math.round(ICON * CENTER_SCALE / 2.0f) + 4;
        graphics.drawCenteredString(font, title, 0, nameY, 0xAAAAAA);
        graphics.drawCenteredString(font, DIAL_HINT, 0, nameY + font.lineHeight + 3, 0x777777);

        pose.popPose();
    }

    private void sign(GuiGraphics graphics, Component text, int x, float scale)
    {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, 0.0f, 0.0f);
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2 + 1, 0xFFFFFF, true);

        pose.popPose();
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        this.mouseX = mouseX;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (scrollY != 0.0) move(step + (scrollY > 0.0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        assert minecraft != null;
        if (!minecraft.options.keyUse.matchesMouse(button)) return super.mouseReleased(mouseX, mouseY, button);

        confirm();
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        assert minecraft != null;
        if (!minecraft.options.keyUse.matches(keyCode, scanCode))
            return super.keyReleased(keyCode, scanCode, modifiers);

        confirm();
        return true;
    }

    private boolean move(int target)
    {
        int next = Mth.clamp(target, 0, Math.max(steps - 1, 0));
        if (next == step) return false;

        slideFrom = next > step ? 1.0f : -1.0f;
        slideStart = Util.getMillis();
        step = next;
        if (slideStart - lastTick >= 45L)
        {
            lastTick = slideStart;
            float fraction = steps <= 1 ? 0.0f : step / (float) (steps - 1);

            assert minecraft != null;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f + fraction * 0.8f, 0.25f));
        }

        return true;
    }

    private void confirm()
    {
        if (step != option.index(option.value()))
            PacketDistributor.sendToServer(new TuneSetPayload(pos, index, option.valueAt(step)));
        onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
