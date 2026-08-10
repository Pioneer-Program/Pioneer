package cute.ame.pioneer.Planet.Common.Event;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Core.API.AuralithAPI;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ZeroGDragEvent
{
  private static final double DRAG = 0.85;
  private static final double MIN_SPEED_SQR = 1.0e-6;

  private ZeroGDragEvent() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Pre event)
  {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    if (AuralithAPI.getGravityFor(player.level().dimension()) != 0.0f) return;

    Vec3 vel = player.getDeltaMovement();
    if (vel.lengthSqr() < MIN_SPEED_SQR) return;

    if (SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player) != null) return;

    player.setDeltaMovement(vel.scale(DRAG));
    player.hurtMarked = true;
  }
}
