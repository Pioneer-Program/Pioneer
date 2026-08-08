package cute.ame.auralithpioneerinitiative.Mixin.Player;

import cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI;
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
    float gravity = AuralithAPI.getGravityFor(self.level().dimension());
    boolean shouldSwim = (gravity == 0.0f);

    if (self.isSwimming() != shouldSwim ^ self.isInWater() || self.onGround()) self.setSwimming(shouldSwim);
  }
}
