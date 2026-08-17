package cute.ame.pioneer.Mixin.Entity;

import cute.ame.pioneer.Core.API.PioneerAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private Level level;

    @Shadow
    public abstract boolean isAddedToLevel();

    @Shadow
    public abstract boolean isRemoved();

    @Inject(method = "setPosRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isAddedToLevel()Z", shift = At.Shift.BEFORE), cancellable = true)
    private void pionner$preventChunkLoad(double x, double y, double z, CallbackInfo ci) {
        if (this.isAddedToLevel() && !this.level.isClientSide && !this.isRemoved() && !PioneerAPI.isSpaceDimension(level.dimension()))
            this.level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4); // Forge - ensure target chunk is loaded.
        ci.cancel();
    }

}
