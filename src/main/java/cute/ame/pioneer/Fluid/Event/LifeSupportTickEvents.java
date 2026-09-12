package cute.ame.pioneer.Fluid.Event;

import cute.ame.pioneer.Fluid.Helper.LifeSupport;

import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class LifeSupportTickEvents
{
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        LifeSupport.tick(player);
    }
}
