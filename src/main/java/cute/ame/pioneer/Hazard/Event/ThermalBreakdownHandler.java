package cute.ame.pioneer.Hazard.Event;

import cute.ame.celsius.Core.Event.ThermalBreakdownEvent;
import cute.ame.pioneer.Pioneer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ThermalBreakdownHandler
{
    @SubscribeEvent
    public static void onBreakdown(ThermalBreakdownEvent event)
    {
        event.level().levelEvent(2001, event.pos(), Block.getId(event.state()));
        event.level().setBlock(event.pos(), event.into(), Block.UPDATE_ALL);
    }
}
