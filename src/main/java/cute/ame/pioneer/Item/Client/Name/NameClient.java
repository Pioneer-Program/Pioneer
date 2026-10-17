package cute.ame.pioneer.Item.Client.Name;

import cute.ame.pioneer.Item.Name.NameServer;
import cute.ame.pioneer.Item.Network.NameLabelsPayload;
import cute.ame.pioneer.Item.Network.NameRequestPayload;
import cute.ame.pioneer.Item.ToolBoxItem;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Registrie.ModToolBoxModules;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class NameClient
{
    private static final Long2ObjectOpenHashMap<Label> LABELS = new Long2ObjectOpenHashMap<>();

    private static double requestX;
    private static double requestZ;
    private static boolean requested;

    public static @Nullable Label label(long pos)
    {
        return LABELS.get(pos);
    }

    public static Long2ObjectMap<Label> labels()
    {
        return LABELS;
    }

    public static void request(LocalPlayer player)
    {
        requested = true;
        requestX = player.getX();
        requestZ = player.getZ();
        PacketDistributor.sendToServer(NameRequestPayload.INSTANCE);
    }

    public static boolean needsRequest(LocalPlayer player, double moved)
    {
        if (!requested) return true;

        double dx = player.getX() - requestX;
        double dz = player.getZ() - requestZ;
        return dx * dx + dz * dz > moved * moved;
    }

    public static void forget()
    {
        requested = false;
    }

    public static void receive(NameLabelsPayload payload)
    {
        if (payload.replace()) LABELS.clear();

        Font font = Minecraft.getInstance().font;
        for (NameLabelsPayload.Entry entry : payload.entries())
        {
            if (entry.label().isEmpty())
            {
                LABELS.remove(entry.pos());
                continue;
            }

            Component text = Component.literal(entry.label());
            LABELS.put(entry.pos(), new Label(entry.label(), text.getVisualOrderText(), font.width(text)));
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!event.isUseItem()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof ToolBoxItem) || ToolBoxItem.moduleOf(stack) != ModToolBoxModules.NAME.get())
            return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        if (!NameServer.nameable(minecraft.level.getBlockEntity(pos))) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        if (minecraft.screen != null) return;

        Label current = LABELS.get(pos.asLong());
        minecraft.setScreen(new NameEditScreen(pos.asLong(), current == null ? "" : current.text(), new ItemStack(minecraft.level.getBlockState(pos).getBlock())));
    }

    public record Label(String text, FormattedCharSequence line, int width)
    {
    }
}
