package cute.ame.pioneer.Item.Client.Tune;

import cute.ame.pioneer.Core.Configure.Tunable;
import cute.ame.pioneer.Core.Configure.TuneOption;
import cute.ame.pioneer.Item.Network.TuneOptionsPayload;
import cute.ame.pioneer.Item.Network.TuneRequestPayload;
import cute.ame.pioneer.Item.Network.TuneSetPayload;
import cute.ame.pioneer.Item.ToolBoxItem;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Registrie.ModToolBoxModules;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class TuneClient
{
    private static boolean pressing;
    private static boolean toggled;
    private static long pressPos;
    private static long pressedAt;
    private static List<TuneOption> options;
    private static long optionsPos;

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!event.isUseItem()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof ToolBoxItem) || ToolBoxItem.moduleOf(stack) != ModToolBoxModules.CONFIGURE.get())
            return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        if (!(minecraft.level.getBlockState(pos).getBlock() instanceof Tunable)) return;

        event.setCanceled(true);
        event.setSwingHand(false);
        if (pressing) return;

        pressing = true;
        pressPos = pos.asLong();
        pressedAt = Util.getMillis();
        options = null;
        toggled = player.isSecondaryUseActive();
        if (toggled)
        {
            event.setSwingHand(true);
            PacketDistributor.sendToServer(new TuneSetPayload(pressPos, TuneSetPayload.TOGGLE, 0.0));
        }
        else PacketDistributor.sendToServer(new TuneRequestPayload(pressPos));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (!pressing) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null)
        {
            pressing = false;
            return;
        }

        if (!minecraft.options.keyUse.isDown())
        {
            pressing = false;
            return;
        }

        if (toggled || options == null || optionsPos != pressPos || Util.getMillis() - pressedAt < 220L) return;

        int primary = primary(options);
        if (primary < 0) return;

        pressing = false;
        TuneOption option = options.get(primary);
        ItemStack icon = new ItemStack(minecraft.level.getBlockState(BlockPos.of(pressPos)).getBlock());
        minecraft.setScreen(option.isChoice() ? new TuneGridScreen(pressPos, primary, option, icon) : new TuneDialScreen(pressPos, primary, option, icon));
    }

    public static void receive(TuneOptionsPayload payload)
    {
        if (payload.pos() != pressPos) return;

        options = payload.options();
        optionsPos = payload.pos();
    }

    private static int primary(List<TuneOption> list)
    {
        int fallback = -1;
        for (int i = 0; i < list.size(); i++)
        {
            TuneOption option = list.get(i);
            if (option.isToggle()) continue;
            if (option.primary()) return i;
            if (fallback < 0) fallback = i;
        }

        return fallback;
    }
}
