package cute.ame.auralithpioneerinitiative.Mixin.Rendering;

import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostRenderer;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.PostProcess.BlackHolePostProcessRenderer;
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
    private void auralith$renderBlackHolePostProcess(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci)
    {
        if (SeamlessGhostRenderer.IN_GHOST_DEBUG_FRAME) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        BlackHolePostProcessRenderer.renderIfActive(camera, frustumMatrix, projectionMatrix, partialTick);
    }
}
