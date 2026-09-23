package cute.ame.pioneer.Gas.Event;

import cute.ame.celsius.Core.Event.SpeciesReloadEvent;
import cute.ame.pioneer.Gas.Loader.GasPropertiesLoader;
import cute.ame.pioneer.Gas.Registry.GasPhysiology;
import cute.ame.pioneer.Pioneer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class GasEvents
{
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event)
    {
        event.addListener(GasPropertiesLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onSpeciesReload(SpeciesReloadEvent event)
    {
        GasPhysiology.rebuild(event.table());
    }
}
