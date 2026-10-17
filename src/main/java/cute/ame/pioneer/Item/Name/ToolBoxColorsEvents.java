package cute.ame.pioneer.Item.Name;

import cute.ame.pioneer.Item.Network.ToolBoxColorsPayload;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ToolBoxColorsEvents
{
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(ToolBoxColorsLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event)
    {
        ToolBoxColorsPayload payload = new ToolBoxColorsPayload(ToolBoxColors.entries());
        ServerPlayer player = event.getPlayer();

        if (player != null) PacketDistributor.sendToPlayer(player, payload);
        else PacketDistributor.sendToAllPlayers(payload);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event)
    {
        ToolBoxColors.invalidate();
    }
}
