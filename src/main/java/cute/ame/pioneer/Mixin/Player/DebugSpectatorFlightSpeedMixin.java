package cute.ame.pioneer.Mixin.Player;

import cute.ame.pioneer.Core.API.AuralithAPI;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class DebugSpectatorFlightSpeedMixin
{
    @Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
    private void auralith$uncapSpectatorSpeed(CallbackInfoReturnable<Float> cir)
    {
        Player self = (Player)(Object) this;
        if(!AuralithAPI.isSpaceDimension(self.level().dimension())) return;

        if (self.isSpectator()) cir.setReturnValue(50000 / 20.f);
    }
}