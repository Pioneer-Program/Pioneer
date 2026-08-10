package cute.ame.pioneer.Mixin.Rendering;

import cute.ame.pioneer.Core.API.AuralithAPI;
import cute.ame.pioneer.Seamless.Client.SeamlessGhostRenderer;
import cute.ame.pioneer.SkyPlanet.Dimension.SpaceDimensionEffect;
import cute.ame.pioneer.SkyPlanet.Rendering.SolarSystemRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SkyMixin
{
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void pioneer$renderSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci)
    {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        if (SeamlessGhostRenderer.IN_GHOST_DEBUG_FRAME)
        {
            ci.cancel();
            return;
        }

        if (level.effects() instanceof SpaceDimensionEffect) return;
        if (!AuralithAPI.hasSkyFor(level.dimension())) return;

        SolarSystemRenderer.getInstance().renderSky(frustumMatrix, projectionMatrix, partialTick, camera, isFoggy, skyFogSetup, level);
        ci.cancel();
    }
}