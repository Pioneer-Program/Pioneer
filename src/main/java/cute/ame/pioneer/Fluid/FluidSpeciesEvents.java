package cute.ame.pioneer.Fluid;

import cute.ame.pioneer.Pioneer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FluidSpeciesEvents
{
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        FluidSpecies.refresh(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() != null) return;
        FluidSpecies.refresh(event.getPlayerList().getServer());
    }
}
