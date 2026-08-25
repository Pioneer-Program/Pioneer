package cute.ame.pioneer.Mixin.Rendering;

import cute.ame.pioneer.Core.Render.Debug.GPUProfiler;
import cute.ame.pioneer.SkyPlanet.Rendering.PostProcess.BlackHolePostProcessRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class BlackHolePostProcessMixin
{
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void pioneer$renderBlackHolePostProcess(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci)
    {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        GPUProfiler.begin("celestial.blackhole");
        BlackHolePostProcessRenderer.renderIfActive(camera, frustumMatrix, projectionMatrix, partialTick);
        GPUProfiler.end();
    }
}
