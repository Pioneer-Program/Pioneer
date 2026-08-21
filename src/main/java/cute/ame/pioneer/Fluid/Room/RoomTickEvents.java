package cute.ame.pioneer.Fluid.Room;

import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class RoomTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        if (rooms == null || rooms.pendingRescans() == 0) return;

        rooms.tick(level);
    }
}