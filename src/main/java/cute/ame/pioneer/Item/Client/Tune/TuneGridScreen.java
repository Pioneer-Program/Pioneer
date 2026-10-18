package cute.ame.pioneer.Item.Client.Tune;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Core.Configure.TuneOption;
import cute.ame.pioneer.Item.Network.TuneSetPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class TuneGridScreen extends Screen
{
    private static final int COLUMNS = 5;
    private static final int CELL = 24;
    private static final int TEXTURE = 32;
    private static final int GAP = 12;
    private static final int ROW_PITCH = CELL + 20;
    private static final int ICON = 16;
    private static final int ICON_GAP = 4;
    private static final float HOVER_SCALE = 1.35f;

    private static final long OPEN_MS = 180L;
    private static final float FOCUS_MS = 60.0f;

    private final long pos;
    private final int index;
    private final ItemStack icon;
    private final int count;
    private final int current;
    private final ResourceLocation[] textures;
    private final Component[] labels;
    private final int[] centerX;
    private final int[] centerY;
    private final float[] focus;

    private int gridTop;
    private int gridBottom;
    private int hovered;
    private long openedAt;
    private long hoveredAt;
    private long lastFrame;

    public TuneGridScreen(long pos, int index, TuneOption option, ItemStack icon)
    {
        super(Component.translatable(option.key()));
        this.pos = pos;
        this.index = index;
        this.icon = icon;
        this.count = option.steps();
        this.current = option.index(option.value());
        this.textures = new ResourceLocation[count];
        this.labels = new Component[count];
        this.centerX = new int[count];
        this.centerY = new int[count];
        this.focus = new float[count];
        this.hovered = current;
        for (int i = 0; i < count; i++)
        {
            String path = option.icon(i);
            textures[i] = path.isEmpty() ? null : ResourceLocation.tryParse(path);
            labels[i] = option.label(i);
        }
    }

    @Override
    protected void init()
    {
        int rows = (count + COLUMNS - 1) / COLUMNS;
        gridTop = height / 2 - rows * ROW_PITCH / 2 - 16;
        gridBottom = gridTop + rows * ROW_PITCH;
        for (int i = 0; i < count; i++)
        {
            int row = i / COLUMNS;
            int inRow = Math.min(COLUMNS, count - row * COLUMNS);
            int rowWidth = inRow * CELL + (inRow - 1) * GAP;

            centerX[i] = (width - rowWidth) / 2 + CELL / 2 + (i % COLUMNS) * (CELL + GAP);
            centerY[i] = gridTop + row * ROW_PITCH + CELL / 2;
        }

        long now = Util.getMillis();
        openedAt = now;
        hoveredAt = now;
        lastFrame = now;
        if (current >= 0 && current < count)
        {
            assert minecraft != null;
            Window window = minecraft.getWindow();
            double scale = window.getGuiScale();
            GLFW.glfwSetCursorPos(window.getWindow(), centerX[current] * scale, centerY[current] * scale);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        long now = Util.getMillis();
        float blend = 1.0f - (float) Math.exp(-(now - lastFrame) / FOCUS_MS);
        lastFrame = now;

        float opened = Mth.clamp((now - openedAt) / (float) OPEN_MS, 0.0f, 1.0f);
        int header = ICON + ICON_GAP + font.width(title);
        int headerX = (width - header) / 2;
        int headerY = gridTop - ICON - 10;
        int headerAlpha = Math.round(opened * 255.0f);
        if (headerAlpha >= 8)
        {
            graphics.renderFakeItem(icon, headerX, headerY);
            graphics.drawString(font, title, headerX + ICON + ICON_GAP, headerY + (ICON - 8) / 2, headerAlpha << 24 | 0xFFFFFF, true);
        }

        RenderSystem.enableBlend();
        PoseStack pose = graphics.pose();
        for (int i = 0; i < count; i++)
        {
            focus[i] += ((i == hovered ? 1.0f : 0.0f) - focus[i]) * blend;
            float appear = Mth.clamp((now - openedAt - i * 25L) / (float) OPEN_MS, 0.0f, 1.0f);
            appear = 1.0f - (1.0f - appear) * (1.0f - appear) * (1.0f - appear);
            if (appear <= 0.0f) continue;

            float scale = Mth.lerp(focus[i], 1.0f, HOVER_SCALE) * appear;
            pose.pushPose();
            pose.translate(centerX[i], centerY[i] - focus[i] * 2.0f, 0.0f);
            pose.scale(scale, scale, 1.0f);
            if (textures[i] != null)
                graphics.blit(textures[i], -CELL / 2, -CELL / 2, CELL, CELL, 0.0f, 0.0f, TEXTURE, TEXTURE, TEXTURE, TEXTURE);
            else graphics.drawCenteredString(font, labels[i], 0, -4, 0xFFFFFF);

            pose.popPose();
        }

        if (hovered >= 0)
        {
            float shown = Mth.clamp((now - hoveredAt) / (float) 150L, 0.0f, 1.0f) * opened;
            int a = Math.round(shown * 255.0f);
            if (a >= 8)
            {
                int y = Math.round(centerY[hovered] + CELL * HOVER_SCALE / 2.0f + 4 + (1.0f - shown) * 3.0f);
                graphics.drawCenteredString(font, labels[hovered], centerX[hovered], y, a << 24 | 0xFFFFFF);
            }
        }

        Component hint = Component.translatable("tune.pioneer.grid_hint", title);
        int hintAlpha = Math.round(opened * 0xAA);
        if (hintAlpha >= 8)
            graphics.drawString(font, hint, (width - font.width(hint)) / 2, gridBottom + 6, hintAlpha << 24 | 0xAAAAAA, true);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {

    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        hover(cellAt(mouseX, mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (scrollY != 0.0 && count > 0)
            hover(Math.floorMod((hovered < 0 ? current : hovered) + (scrollY > 0.0 ? -1 : 1), count));
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

    private int cellAt(double mouseX, double mouseY)
    {
        for (int i = 0; i < count; i++)
        {
            if (Math.abs(mouseX - centerX[i]) <= (CELL + GAP) / 2.0 && Math.abs(mouseY - centerY[i]) <= ROW_PITCH / 2.0)
                return i;
        }

        return -1;
    }

    private void hover(int cell)
    {
        if (cell == hovered) return;

        hovered = cell;
        hoveredAt = Util.getMillis();

        if (cell >= 0)
        {
            assert minecraft != null;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f + 0.8f * cell / Math.max(count - 1, 1), 0.25f));
        }
    }

    private void confirm()
    {
        if (hovered >= 0 && hovered != current) PacketDistributor.sendToServer(new TuneSetPayload(pos, index, hovered));
        onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
