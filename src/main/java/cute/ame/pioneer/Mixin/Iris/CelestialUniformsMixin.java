package cute.ame.pioneer.Mixin.Iris;

import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Physics.WorldClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CelestialUniforms", remap = false)
public class CelestialUniformsMixin
{
    @Inject(method = "isDay", at = @At("HEAD"), cancellable = true)
    private static void pioneer$isDayFromPhysicalSun(CallbackInfoReturnable<Boolean> cir)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Optional<WorldClock.Sample> sample = WorldClock.sample(level, 0.0);
        if (sample.isEmpty()) return;

        cir.setReturnValue(sample.get().sunUp() > 0.0);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, index = 1)
    private static float pioneer$tiltFromHostBody(float packValue)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return packValue;
        return WorldClock.surfaceHost(level).map(PlanetDefinition::axialTilt).orElse(packValue);
    }
}
