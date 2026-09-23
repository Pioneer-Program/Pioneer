package cute.ame.pioneer.Hazard.Event;

import cute.ame.pioneer.Config;
import cute.ame.celsius.Core.Event.VesselBurstEvent;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class VesselBurstHandler
{
    @SubscribeEvent
    public static void onBurst(VesselBurstEvent.Post event)
    {
        ServerLevel level = event.level();
        BlockPos pos = event.pos();
        level.destroyBlock(pos, false);

        float power = (float) Math.min(Config.BURST_EXPLOSION_POWER.get() * event.overload(), Config.BURST_EXPLOSION_MAX_POWER.get());
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);

        Pioneer.LOGGER.debug("[Pioneer] burst blast at {} (power {})", pos, String.format("%.2f", power));
    }
}
