package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.RingDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.RenderingHelper.ShaderHelper;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.RingTextureHelper;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

import java.util.Random;

import static cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.CelestialMath.lerp;

public final class RingMeshRenderer
{
    private static final float RING_PLANET_SHADOW_FLOOR = 0.15f;
    private static final float RING_SHADOW_SOFTNESS_FRAC = 0.25f;

    @Deprecated
    public static void render(PoseStack ps, RingDefinition rings, float apparentSize)
    {
        render(ps, rings, apparentSize, 0f, 0f, 0f);
    }

    public static void render(PoseStack ps, RingDefinition rings, float apparentSize, float sunDirX, float sunDirY, float sunDirZ)
    {
        float[] sun = normalizeOrZero(sunDirX, sunDirY, sunDirZ);
        sunDirX = sun[0]; sunDirY = sun[1]; sunDirZ = sun[2];

        setupRingRenderState();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        Random rng = new Random(rings.seed());
        Tesselator tess = Tesselator.getInstance();
        VertexFormat fmt = ShaderHelper.shadersActive() ? DefaultVertexFormat.NEW_ENTITY : DefaultVertexFormat.POSITION_TEX_COLOR;
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, fmt);
        float planetRadius = apparentSize * 0.5f;
        boolean shadowed = (sunDirX != 0f || sunDirY != 0f || sunDirZ != 0f);
        if (ShaderHelper.shadersActive()) emitRingQuads(buf, ps, rings.innerRadius() * planetRadius, rings.outerRadius() * planetRadius, Math.max(1, rings.ringCount()), rings, rng, shadowed, sunDirX, sunDirY, sunDirZ, planetRadius);
        else
        {
            Matrix4f m = ps.last().pose();
            emitRingQuads(buf, m, rings.innerRadius() * planetRadius, rings.outerRadius() * planetRadius, Math.max(1, rings.ringCount()), rings, rng, shadowed, sunDirX, sunDirY, sunDirZ, planetRadius);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        teardownRingRenderState();
    }

    @Deprecated
    public static void render(PoseStack ps, RingDefinition rings, float apparentSize, float localCamX, float localCamY, float localCamZ, boolean front)
    {
        render(ps, rings, apparentSize, localCamX, localCamY, localCamZ, front, 0f, 0f, 0f);
    }

    public static void render(PoseStack ps, RingDefinition rings, float apparentSize, float localCamX, float localCamY, float localCamZ, boolean front, float sunDirX, float sunDirY, float sunDirZ)
    {
        float[] sun = normalizeOrZero(sunDirX, sunDirY, sunDirZ);
        sunDirX = sun[0]; sunDirY = sun[1]; sunDirZ = sun[2];

        setupRingRenderState();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        float planetRadius = apparentSize * 0.5f;
        float innerR = rings.innerRadius() * planetRadius;
        float outerR = rings.outerRadius() * planetRadius;
        int count = Math.max(1, rings.ringCount());
        boolean shadowed = (sunDirX != 0f || sunDirY != 0f || sunDirZ != 0f);

        float[] quadDotX = {  0,  0, -1, +1, -1, +1, -1, +1 };
        float[] quadDotZ = { -1, +1,  0,  0, -1, -1, +1, +1 };

        int light   = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        Random rng = new Random(rings.seed());
        Tesselator tess = Tesselator.getInstance();
        VertexFormat fmt = ShaderHelper.shadersActive() ? DefaultVertexFormat.NEW_ENTITY : DefaultVertexFormat.POSITION_TEX_COLOR;
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, fmt);
        Matrix4f m = ps.last().pose();

        for (int ri = 0; ri < count; ri++)
        {
            float r0 = lerp(innerR, outerR, (float) ri / count);
            float r1 = lerp(innerR, outerR, (float) (ri + 1) / count);
            float rndT = rng.nextFloat();
            float cr = lerp(rings.minR(), rings.maxR(), rndT);
            float cg = lerp(rings.minG(), rings.maxG(), rndT);
            float cb = lerp(rings.minB(), rings.maxB(), rndT);
            float ca = rings.opacity() * (0.6f + 0.4f * rng.nextFloat());

            float[][][] quads =
            {
                {{ -r0,0,-r1 },{  r0,0,-r1 },{  r0,0,-r0 },{ -r0,0,-r0 }},
                {{ -r0,0, r0 },{  r0,0, r0 },{  r0,0, r1 },{ -r0,0, r1 }},
                {{ -r1,0,-r0 },{ -r0,0,-r0 },{ -r0,0, r0 },{ -r1,0, r0 }},
                {{  r0,0,-r0 },{  r1,0,-r0 },{  r1,0, r0 },{  r0,0, r0 }},
                {{ -r1,0,-r1 },{ -r0,0,-r1 },{ -r0,0,-r0 },{ -r1,0,-r0 }},
                {{  r0,0,-r1 },{  r1,0,-r1 },{  r1,0,-r0 },{  r0,0,-r0 }},
                {{ -r1,0, r0 },{ -r0,0, r0 },{ -r0,0, r1 },{ -r1,0, r1 }},
                {{  r0,0, r0 },{  r1,0, r0 },{  r1,0, r1 },{  r0,0, r1 }},
            };

            for (int qi = 0; qi < quads.length; qi++)
            {
                float dot = quadDotX[qi] * localCamX + quadDotZ[qi] * localCamZ;
                if ((dot >= 0) != front) continue;

                for (float[] v : quads[qi])
                {
                    float shade = shadowed ? planetShadowLit(v[0], v[2], sunDirX, sunDirY, sunDirZ, planetRadius) : 1.0f;
                    buf.addVertex(m, v[0], v[1], v[2]).setColor(cr*shade, cg*shade, cb*shade, ca).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(ps.last(), 0, 1, 0);
                }
            }
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        teardownRingRenderState();
    }

    private static void setupRingRenderState()
    {
        RenderSystem.setShader(ShaderHelper.shadersActive() ? GameRenderer::getRendertypeEntitySolidShader : GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, RingTextureHelper.getWhite());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.disableCull();
    }

    private static void teardownRingRenderState()
    {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static float planetShadowLit(float x, float z, float sunDirX, float sunDirY, float sunDirZ, float planetRadius)
    {
        float dotPS = x * sunDirX + z * sunDirZ;
        float distSq = x * x + z * z;

        float dSq = distSq - dotPS * dotPS;
        float d = (float) Math.sqrt(Math.max(0f, dSq));

        float soft = Math.max(planetRadius * RING_SHADOW_SOFTNESS_FRAC, 1e-4f);
        float dMiss = smoothstep(planetRadius - soft, planetRadius + soft, d);
        float facingSun = smoothstep(-soft, soft, dotPS);

        float litness = 1.0f - (1.0f - dMiss) * (1.0f - facingSun);
        return RING_PLANET_SHADOW_FLOOR + (1.0f - RING_PLANET_SHADOW_FLOOR) * litness;
    }

    private static float[] normalizeOrZero(float x, float y, float z)
    {
        float lenSq = x * x + y * y + z * z;
        if (lenSq < 1e-12f) return new float[] { 0f, 0f, 0f };
        float inv = 1.0f / (float) Math.sqrt(lenSq);
        return new float[] { x * inv, y * inv, z * inv };
    }

    private static float smoothstep(float edge0, float edge1, float x)
    {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0f - 2.0f * t);
    }

    private static float clamp01(float v)
    {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static void emitRingQuads(BufferBuilder buf, Matrix4f m, float innerR, float outerR, int count, RingDefinition rings, Random rng, boolean shadowed, float sunDirX, float sunDirY, float sunDirZ, float planetRadius)
    {
        for (int ri = 0; ri < count; ri++)
        {
            float r0 = lerp(innerR, outerR, (float) ri / count);
            float r1 = lerp(innerR, outerR, (float) (ri + 1) / count);
            float rndT = rng.nextFloat();
            float cr = lerp(rings.minR(), rings.maxR(), rndT);
            float cg = lerp(rings.minG(), rings.maxG(), rndT);
            float cb = lerp(rings.minB(), rings.maxB(), rndT);
            float ca = rings.opacity() * (0.6f + 0.4f * rng.nextFloat());

            float[][] verts =
            {
                {-r0,0,-r1}, { r0,0,-r1}, { r0,0,-r0}, {-r0,0,-r0},
                {-r0,0, r0}, { r0,0, r0}, { r0,0, r1}, {-r0,0, r1},
                {-r1,0,-r0}, {-r0,0,-r0}, {-r0,0, r0}, {-r1,0, r0},
                { r0,0,-r0}, { r1,0,-r0}, { r1,0, r0}, { r0,0, r0},
                {-r1,0,-r1}, {-r0,0,-r1}, {-r0,0,-r0}, {-r1,0,-r0},
                { r0,0,-r1}, { r1,0,-r1}, { r1,0,-r0}, { r0,0,-r0},
                {-r1,0, r0}, {-r0,0, r0}, {-r0,0, r1}, {-r1,0, r1},
                { r0,0, r0}, { r1,0, r0}, { r1,0, r1}, { r0,0, r1},
            };

            for (float[] v : verts)
            {
                float shade = shadowed ? planetShadowLit(v[0], v[2], sunDirX, sunDirY, sunDirZ, planetRadius) : 1.0f;
                buf.addVertex(m, v[0], v[1], v[2]).setUv(0,0).setColor(cr*shade, cg*shade, cb*shade, ca);
            }
        }
    }

    private static void emitRingQuads(BufferBuilder buf, PoseStack ps, float innerR, float outerR, int count, RingDefinition rings, Random rng, boolean shadowed, float sunDirX, float sunDirY, float sunDirZ, float planetRadius)
    {
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        Matrix4f m  = ps.last().pose();

        for (int ri = 0; ri < count; ri++)
        {
            float r0 = lerp(innerR, outerR, (float) ri / count);
            float r1 = lerp(innerR, outerR, (float)(ri + 1) / count);
            float rndT = rng.nextFloat();
            float cr = lerp(rings.minR(), rings.maxR(), rndT);
            float cg = lerp(rings.minG(), rings.maxG(), rndT);
            float cb = lerp(rings.minB(), rings.maxB(), rndT);
            float ca = rings.opacity() * (0.6f + 0.4f * rng.nextFloat());

            float[][] verts =
            {
                {-r0,0,-r1}, { r0,0,-r1}, { r0,0,-r0}, {-r0,0,-r0},
                {-r0,0, r0}, { r0,0, r0}, { r0,0, r1}, {-r0,0, r1},
                {-r1,0,-r0}, {-r0,0,-r0}, {-r0,0, r0}, {-r1,0, r0},
                { r0,0,-r0}, { r1,0,-r0}, { r1,0, r0}, { r0,0, r0},
                {-r1,0,-r1}, {-r0,0,-r1}, {-r0,0,-r0}, {-r1,0,-r0},
                { r0,0,-r1}, { r1,0,-r1}, { r1,0,-r0}, { r0,0,-r0},
                {-r1,0, r0}, {-r0,0, r0}, {-r0,0, r1}, {-r1,0, r1},
                { r0,0, r0}, { r1,0, r0}, { r1,0, r1}, { r0,0, r1},
            };

            for (float[] v : verts)
            {
                float shade = shadowed ? planetShadowLit(v[0], v[2], sunDirX, sunDirY, sunDirZ, planetRadius) : 1.0f;
                float rr = cr*shade, gg = cg*shade, bb = cb*shade;
                if (ShaderHelper.shadersActive()) buf.addVertex(m, v[0], v[1], v[2]).setColor(rr,gg,bb,ca).setUv(0,0).setOverlay(overlay).setLight(light).setNormal(ps.last(), 0,1,0);
                else buf.addVertex(m, v[0], v[1], v[2]).setUv(0,0).setColor(rr,gg,bb,ca);
            }
        }
    }
}