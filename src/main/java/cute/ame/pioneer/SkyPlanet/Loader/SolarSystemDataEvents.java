package cute.ame.pioneer.SkyPlanet.Loader;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Network.GasSyncPayload;
import cute.ame.pioneer.SkyPlanet.Network.SolarSystemSyncPayload;
import cute.ame.pioneer.SkyPlanet.Registry.GasRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class SolarSystemDataEvents
{
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(SolarSystemLoader.INSTANCE);
        event.addListener(GasLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event)
    {
        SolarSystemSyncPayload payload = new SolarSystemSyncPayload(Map.copyOf(PioneerAPI.getAllSystems()));
        GasSyncPayload gasPayload = new GasSyncPayload(Map.copyOf(GasRegistry.snapshot()));
        ServerPlayer player = event.getPlayer();

        if (player != null)
        {
            PacketDistributor.sendToPlayer(player, payload);
            PacketDistributor.sendToPlayer(player, gasPayload);
        }
        else for (ServerPlayer online : event.getPlayerList().getPlayers())
        {
            PacketDistributor.sendToPlayer(online, payload);
            PacketDistributor.sendToPlayer(online, gasPayload);
        }
    }
}
