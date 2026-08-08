package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.SunDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector.Projected;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Star.BuiltinStarTypes;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Star.StarTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import static cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.CelestialMath.poseAxialRotation;

public final class SunRenderer
{
    public static void renderRealScale(PoseStack ps, SunDefinition sun, long tick, float partialTick, double dx, double dy, double dz, double dist, Vec3 realCamPos)
    {
        Projected proj = ShellProjector.projectToSafeShell(dx, dy, dz, dist, (float) Math.max(sun.size(), ShellProjector.MIN_APPARENT));

        ps.pushPose();
        ps.translate(proj.dx, proj.dy, proj.dz);
        poseAxialRotation(ps, sun.axialRotationSpeed(), tick, partialTick);

        boolean rendersAsPostProcess = sun.type().equals(BuiltinStarTypes.BLACK_HOLE);
        ps.pushPose();
        ps.scale(proj.size, proj.size, proj.size);
        if (sun.glowLayers() > 0 && !rendersAsPostProcess) renderGlow(ps, sun, 1.0f);
        ps.popPose();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        StarTypeRegistry.get(sun.type()).renderExtras(ps, sun, tick, partialTick, proj.size * sun.size(), bufferSource, dx, dy, dz, realCamPos);

        ps.popPose();
    }

    public static void renderGlow(PoseStack ps, SunDefinition sun, float apparentScale)
    {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.disableCull();

        float r = sun.glowR(), g = sun.glowG(), b = sun.glowB();
        int layers = sun.glowLayers();
        float coreSize = sun.size() * apparentScale;
        float glowScale = sun.glowScale();

        for (int i = layers; i > 0; i--)
        {
            float t = (float) i / layers;
            float layerSize = coreSize * (1.0f + t * (glowScale - 1.0f));
            float alpha = (1.0f - t) * 0.06f;
            ps.pushPose();
            ps.scale(layerSize, layerSize, layerSize);
            CubeMeshRenderer.renderColoredCube(ps, r, g, b, alpha);
            ps.popPose();
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }
}