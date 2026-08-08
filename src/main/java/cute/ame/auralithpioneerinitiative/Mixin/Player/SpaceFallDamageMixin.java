package cute.ame.auralithpioneerinitiative.Mixin.Player;

import cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SpaceFallDamageMixin
{
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void auralith$cancelVoidDamage(CallbackInfo ci)
    {
        Entity self = (Entity) (Object) this;
        if (AuralithAPI.isSpaceDimension(self.level().dimension())) ci.cancel();
    }
}