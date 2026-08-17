package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Core.Render.Helper.CubeGeometry;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;

public final class StarRenderer
{
    private static final ResourceLocation STAR = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "star");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(STAR);
    }

    private static final float FALLOFF = 3.6f;
    private static final float GRANULATION = 0.45f;
    private static final float CHROMOSPHERE = 0.9f;
    private static final float PROMINENCE = 0.7f;
    private static final float RAY_STRENGTH = 0.8f;
    private static final float INTENSITY = 2.6f;
    private static final float SQUARENESS = 1.0f;

    public static void render(PoseStack ps, SunDefinition sun, float camObjX, float camObjY, float camObjZ, float coreSize, float spin, float timeSeconds)
    {
        final float glowScale = Math.max(sun.glowScale(), 1.05f);
        final float bounds = coreSize * glowScale;
        if (bounds <= 0.0f) return;

        final float glowRadius = 0.5f;
        final float coreRadius = glowRadius / glowScale;
        final float invScale = 1.0f / bounds;

        final Matrix4f model = new Matrix4f(ps.last().pose()).scale(bounds);

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);

        VeilSkyShaderHelper.draw(
        STAR,
        shader ->
        {
            set(shader, "uStarModel", model);
            set(shader, "uCamPos", camObjX * invScale, camObjY * invScale, camObjZ * invScale);
            set(shader, "uCoreRadius", coreRadius);
            set(shader, "uGlowRadius", glowRadius);
            set(shader, "uCoreColor", sun.glowR(), sun.glowG(), sun.glowB());
            set(shader, "uGlowColor", sun.glowR(), sun.glowG(), sun.glowB());
            set(shader, "uIntensity", INTENSITY);
            set(shader, "uFalloff", FALLOFF);
            set(shader, "uGranulation", GRANULATION);
            set(shader, "uSquareness", SQUARENESS);
            set(shader, "uWhiteHot", sun.whiteHot());
            set(shader, "uChromosphere", CHROMOSPHERE);
            set(shader, "uProminence", PROMINENCE);
            set(shader, "uRayStrength", RAY_STRENGTH);
            set(shader, "uSpin", spin);
            set(shader, "uTime", timeSeconds);
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            CubeGeometry.emit(buf, 1.0f);
            renderType.draw(buf.buildOrThrow());
        });

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }
}
