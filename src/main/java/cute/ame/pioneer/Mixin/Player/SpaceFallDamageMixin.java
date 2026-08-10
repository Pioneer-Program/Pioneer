package cute.ame.pioneer.Mixin.Player;

import cute.ame.pioneer.Core.API.PioneerAPI;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SpaceFallDamageMixin
{
    @Inject(method = "checkBelowWorld", at = @At("HEAD"), cancellable = true)
    private void pioneer$cancelVoidDamage(CallbackInfo ci)
    {
        Entity self = (Entity) (Object) this;
        if (PioneerAPI.isSpaceDimension(self.level().dimension())) ci.cancel();
    }
}