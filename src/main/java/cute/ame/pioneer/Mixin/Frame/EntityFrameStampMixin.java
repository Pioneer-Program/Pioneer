package cute.ame.pioneer.Mixin.Frame;

import cute.ame.pioneer.Frame.FrameEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityFrameStampMixin
{
    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void pioneer$stampFrameCell(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir)
    {
        FrameEvents.stamp((Entity) (Object) this);
    }
}
