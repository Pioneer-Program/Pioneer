package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class SeamlessTickHandler
{
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (event.getEntity() instanceof ServerPlayer player) TransitionManager.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) TransitionManager.onPlayerDisconnect(player);
    }
}
