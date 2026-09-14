package cute.ame.pioneer.Thermal.Event;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import cute.ame.pioneer.Thermal.Loader.ThermalMaterialLoader;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterials;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ThermalMaterialEvents
{
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(ThermalMaterialLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ThermalMaterials.refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() != null) return;

        MinecraftServer server = event.getPlayerList().getServer();
        ThermalMaterials.refresh(server);
        ThermalLevelData.onMaterialsReloaded(server);
    }
}
