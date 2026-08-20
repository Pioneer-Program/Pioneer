package cute.ame.pioneer.Mixin.World;

import cute.ame.pioneer.SkyPlanet.Physics.WorldClock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelTimeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LevelTimeAccess.class)
public interface TimeOfDayMixin
{
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void pioneer$physicalTimeOfDay(float partialTick, CallbackInfoReturnable<Float> cir)
    {
        if (!(this instanceof Level self)) return;

        Optional<Double> frac = WorldClock.surfaceDayFraction(self, partialTick);
        if (frac.isEmpty()) return;

        cir.setReturnValue(frac.get().floatValue());
    }
}