package cute.ame.pioneer.Event;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Core.API.AuralithAPI;
import cute.ame.pioneer.Item.PortableThrusterItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public class PortableThrusterEvent
{
  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event)
  {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    if (!player.isUsingItem()) return;
    if (!(player.getUseItem().getItem() instanceof PortableThrusterItem)) return;

    AuralithAPI.getBindingForDimension(player.level().dimension()).ifPresent(surface ->
    {
      if(!surface.isSurfaceDimension()) applyThrust(player);
    });
  }

  private static void applyThrust(ServerPlayer player)
  {
    Vec3 look = player.getLookAngle();
    Vec3 current = player.getDeltaMovement();
    Vec3 next = current.add(look.scale(PortableThrusterItem.ACCELERATION));

    next = clampSpeed(next, PortableThrusterItem.MAX_SPEED);
    player.setDeltaMovement(next);
    player.hurtMarked = true;
  }

  private static Vec3 clampSpeed(Vec3 v, double limit)
  {
    return new Vec3(clamp(v.x, limit),clamp(v.y, limit),clamp(v.z, limit));
  }

  private static double clamp(double value, double limit)
  {
    return Math.max(-limit, Math.min(limit, value));
  }
}
