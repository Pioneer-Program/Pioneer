package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Pioneer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class SeamlessTickHandler
{
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        TransitionManager.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServerLookup.withServer(player, server -> TransitionManager.onPlayerDisconnect(player, dimKey -> server.getLevel(dimKey)));
    }

    private static final class MinecraftServerLookup
    {
        static void withServer(ServerPlayer player, Consumer<MinecraftServer> action)
        {
            MinecraftServer server = player.getServer();
            if (server != null) action.accept(server);
        }
    }
}