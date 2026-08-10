package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.vertex.*;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.SkyPlanet.Data.RingDefinition;
import net.minecraft.resources.ResourceLocation;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;

public final class RingShaderRenderer
{
    private static final ResourceLocation RINGS_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath("pioneer", "rings");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(RINGS_RENDER_TYPE);
    }

    private static final float RING_SHADOW_SOFTNESS_FRAC = 0.06f;
    private static final float RING_PLANET_SHADOW_FLOOR = 0.15f;
    private static final float BAND_CONTRAST = 0.45f;

    public static void render(RingDefinition rings, float apparentSize, float sunDirX, float sunDirY, float sunDirZ)
    {
        final float len = (float) Math.sqrt(sunDirX * sunDirX + sunDirY * sunDirY + sunDirZ * sunDirZ);
        final float sx, sy, sz;
        if (len < 1e-6f) { sx = 0f; sy = 0f; sz = 0f; }
        else { float inv = 1f / len; sx = sunDirX * inv; sy = sunDirY * inv; sz = sunDirZ * inv; }

        final float planetRadius = apparentSize * 0.5f;
        final float innerR = rings.innerRadius() * planetRadius;
        final float outerR = rings.outerRadius() * planetRadius;
        final int bands = Math.max(1, rings.ringCount());

        final float cr = (rings.minR() + rings.maxR()) * 0.5f;
        final float cg = (rings.minG() + rings.maxG()) * 0.5f;
        final float cb = (rings.minB() + rings.maxB()) * 0.5f;
        final float ca = rings.opacity();

        final float seed = (float) (rings.seed() & 0xFFFF);

        VeilSkyShaderHelper.draw(
        RINGS_RENDER_TYPE,
        shader ->
        {
            set(shader, "uSunDir", sx, sy, sz);
            set(shader, "uPlanetRadius", planetRadius);
            set(shader, "uInnerR", innerR);
            set(shader, "uOuterR", outerR);
            set(shader, "uShadowSoftFrac", RING_SHADOW_SOFTNESS_FRAC);
            set(shader, "uShadowFloor", RING_PLANET_SHADOW_FLOOR);
            set(shader, "uBandCount", (float) bands);
            set(shader, "uBandContrast", BAND_CONTRAST);
            set(shader, "uSeed", seed);
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            emitDisc(buf, outerR, cr, cg, cb, ca);
            renderType.draw(buf.buildOrThrow());
        });
    }

    private static void emitDisc(BufferBuilder buf, float r, float cr, float cg, float cb, float ca)
    {
        buf.addVertex(-r, 0f, -r).setUv(0f, 0f).setColor(cr, cg, cb, ca).setNormal(0f, 1f, 0f);
        buf.addVertex( r, 0f, -r).setUv(1f, 0f).setColor(cr, cg, cb, ca).setNormal(0f, 1f, 0f);
        buf.addVertex( r, 0f,  r).setUv(1f, 1f).setColor(cr, cg, cb, ca).setNormal(0f, 1f, 0f);
        buf.addVertex(-r, 0f,  r).setUv(0f, 1f).setColor(cr, cg, cb, ca).setNormal(0f, 1f, 0f);
    }
}
