package cute.ame.pioneer.LifeSupport.Event;

import cute.ame.pioneer.LifeSupport.Level.KelpBeds;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class KelpBedTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        KelpBeds beds = KelpBeds.getIfPresent(level);
        if (beds != null) beds.tick(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if (event.getLevel() instanceof ServerLevel level) KelpBeds.unload(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        KelpBeds.clear();
    }
}
