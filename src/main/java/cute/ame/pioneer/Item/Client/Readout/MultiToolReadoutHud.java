package cute.ame.pioneer.Item.Client.Readout;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Core.Readout.Readout;
import cute.ame.pioneer.Core.Readout.ReadoutSource;
import cute.ame.pioneer.Core.Readout.ReadoutUnit;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.Network.MultiToolReadoutPayload;
import cute.ame.pioneer.Item.Network.MultiToolWatchPayload;
import cute.ame.pioneer.Pioneer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class MultiToolReadoutHud
{
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_readout");

    private static final int LINE_HEIGHT = 10;
    private static final int ICON = 16;
    private static final int ICON_GAP = 4;
    private static final int HEADER = ICON + 3;
    private static final int INDENT = 2;
    private static final int OFFSET_X = 20;
    private static final int OFFSET_Y = -6;
    private static final int Z = 400;

    private static final FormattedCharSequence[] LINES = new FormattedCharSequence[Readout.MAX_LINES];

    private static boolean watching;
    private static long watched;

    private static FormattedCharSequence title;
    private static ItemStack icon = ItemStack.EMPTY;
    private static Block shownBlock;
    private static int count;
    private static int width;

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, MultiToolReadoutHud::render);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        boolean target = false;
        long pos = 0L;

        if (player != null && minecraft.level != null && !MultiToolItem.held(player).isEmpty() && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK)
        {
            BlockPos aimed = hit.getBlockPos();
            if (minecraft.level.getBlockState(aimed).getBlock() instanceof ReadoutSource)
            {
                target = true;
                pos = aimed.asLong();
            }
        }

        if (target == watching && (!target || pos == watched)) return;

        watching = target;
        watched = pos;
        title = null;
        count = 0;
        if (player != null) PacketDistributor.sendToServer(new MultiToolWatchPayload(target, pos));
    }

    public static void receive(MultiToolReadoutPayload payload)
    {
        if (!watching || payload.pos() != watched) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (payload.lines().isEmpty())
        {
            title = null;
            count = 0;
            return;
        }

        Font font = minecraft.font;
        Block block = minecraft.level.getBlockState(BlockPos.of(watched)).getBlock();
        if (block != shownBlock)
        {
            shownBlock = block;
            icon = new ItemStack(block);
        }

        Component name = block.getName();
        title = name.getVisualOrderText();
        width = ICON + ICON_GAP + font.width(name);
        count = Math.min(payload.lines().size(), Readout.MAX_LINES);

        for (int i = 0; i < count; i++)
        {
            Component line = format(payload.lines().get(i));
            LINES[i] = line.getVisualOrderText();
            width = Math.max(width, INDENT + font.width(line));
        }
    }

    private static Component format(MultiToolReadoutPayload.Line line)
    {
        MutableComponent text = Component.translatable(line.key()).withStyle(ChatFormatting.GRAY).append(Component.literal(": ").withStyle(ChatFormatting.GRAY));

        if (line.kind() == Readout.NUMBER)
        {
            if (!Double.isFinite(line.value()))
                return text.append(Component.translatable("readout.pioneer.none").withStyle(ChatFormatting.DARK_GRAY));

            ReadoutUnit unit = ReadoutUnit.byIndex(line.unit());
            return text.append(Component.literal(unit.number(line.value())).withStyle(ChatFormatting.AQUA)).append(Component.literal(unit.suffix()).withStyle(ChatFormatting.DARK_GRAY));
        }

        Component value = line.kind() == Readout.KEY ? Component.translatable(line.text()) : Component.literal(line.text());
        return text.append(value.copy().withStyle(ChatFormatting.AQUA));
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta)
    {
        if (title == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null || MultiToolItem.held(minecraft.player).isEmpty())
            return;

        int height = HEADER + count * LINE_HEIGHT;
        int x = graphics.guiWidth() / 2 + OFFSET_X;
        int y = graphics.guiHeight() / 2 + OFFSET_Y;

        TooltipRenderUtil.renderTooltipBackground(graphics, x, y, width, height, Z);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0.0f, 0.0f, Z);

        Font font = minecraft.font;
        graphics.renderFakeItem(icon, x, y);
        graphics.drawString(font, title, x + ICON + ICON_GAP, y + (ICON - font.lineHeight) / 2 + 1, 0xFFFFFF, true);

        int lineY = y + HEADER;
        for (int i = 0; i < count; i++, lineY += LINE_HEIGHT)
            graphics.drawString(font, LINES[i], x + INDENT, lineY, 0xAAAAAA, true);

        pose.popPose();
    }
}
