package cute.ame.auralithpioneerinitiative.Planet.Common.Event;

import cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Auralithpioneerinitiative.MODID)
public class ZeroGDragEvent
{
  private static final double DRAG = 0.85;

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Pre event)
  {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    float gravity = AuralithAPI.getGravityFor(player.level().dimension());

    boolean isZeroG = (gravity == 0.0f);
    if (gravity != 0.0f) return;
    Vec3 vel = player.getDeltaMovement();

    if (player.isSwimming() != isZeroG) player.setSwimming(isZeroG);
    if (vel.lengthSqr() < 1e-6) return;

    player.setDeltaMovement(vel.scale(DRAG));
    player.hurtMarked = true;
  }
}
