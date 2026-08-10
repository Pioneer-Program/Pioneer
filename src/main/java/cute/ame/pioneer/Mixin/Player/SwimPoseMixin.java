package cute.ame.pioneer.Mixin.Player;

import cute.ame.pioneer.Core.API.AuralithAPI;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class SwimPoseMixin
{
  @Inject(method = "aiStep", at = @At("RETURN"))
  private void auralith$overrideSwimPose(CallbackInfo ci)
  {
    Player self = (Player) (Object) this;
    if (self.isInWater()) return;

    boolean shouldSwim = AuralithAPI.getGravityFor(self.level().dimension()) == 0.0f;
    if (self.isSwimming() != shouldSwim) self.setSwimming(shouldSwim);
  }
}
