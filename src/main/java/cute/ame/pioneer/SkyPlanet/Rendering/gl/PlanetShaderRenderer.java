package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.vertex.*;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Core.Render.Helper.CloudShadowParams;
import cute.ame.pioneer.Core.Render.Helper.CubeGeometry;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.Core.Render.Helper.SamplerBinder;
import net.minecraft.resources.ResourceLocation;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;

public final class PlanetShaderRenderer
{
    private static final ResourceLocation PLANET_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath("pioneer", "planet");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(PLANET_RENDER_TYPE);
    }

    public static final float NO_RINGS = -1.0f;
    private static final float NIGHT_FACE_FLOOR = 0.03f;
    private static final float FACE_TERMINATOR_SOFTNESS = 0.35f;
    private static final float RING_SHADOW_SOFTNESS_FRAC = 0.06f;

    private static final float CLOUD_SHADOW_STRENGTH = 0.55f;

    private static final int UNIT_FACE_TEX = 0;
    private static final int UNIT_WEATHER_LUT = 5;

    private static final float[] FACE_NORMALS =
    {
         1,  0,  0,
        -1,  0,  0,
         0,  1,  0,
         0, -1,  0,
         0,  0,  1,
         0,  0, -1,
    };

    public static void render(CubemapTextures cubemap, float sunDirX, float sunDirY, float sunDirZ, float alpha, float ringInnerR, float ringOuterR)
    {
        final ResourceLocation[] faceTex =
        {
            cubemap.right(), cubemap.left(),
            cubemap.top(), cubemap.bottom(),
            cubemap.front(), cubemap.back(),
        };

        final boolean ringShadow = ringInnerR > 0f && ringOuterR > ringInnerR;
        final float innerR = ringShadow ? ringInnerR : NO_RINGS;
        final float outerR = ringShadow ? ringOuterR : NO_RINGS;

        final boolean cloudShadow = CloudShadowParams.valid();
        final ResourceLocation weatherLut = CloudShadowParams.weatherLut();

        for (int face = 0; face < 6; face++)
        {
            final int f = face;
            VeilSkyShaderHelper.draw(
            PLANET_RENDER_TYPE,
            shader ->
            {
                SamplerBinder.bind(shader, "uFaceTex", faceTex[f], UNIT_FACE_TEX);

                set(shader, "uSunDir", sunDirX, sunDirY, sunDirZ);
                set(shader, "uAlpha", alpha);

                set(shader, "uTerminatorSoftness", FACE_TERMINATOR_SOFTNESS);
                set(shader, "uNightFloor", NIGHT_FACE_FLOOR);

                set(shader, "uRingInnerR", innerR);
                set(shader, "uRingOuterR", outerR);
                set(shader, "uRingShadowSoftFrac", RING_SHADOW_SOFTNESS_FRAC);

                if (cloudShadow)
                {
                    SamplerBinder.bind(shader, "uWeatherLUT", weatherLut, UNIT_WEATHER_LUT);
                    set(shader, "uCloudShadowStrength", CLOUD_SHADOW_STRENGTH);
                    set(shader, "uCoverageBias", CloudShadowParams.coverageBias());
                    set(shader, "uWeatherDriftSC", CloudShadowParams.driftSin(), CloudShadowParams.driftCos());
                    set(shader, "uCloudInner", CloudShadowParams.cloudInner());
                }
                else
                {
                    set(shader, "uCloudShadowStrength", 0.0f);
                }
            },
            renderType ->
            {
                BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
                emitFace(buf, f, alpha);
                renderType.draw(buf.buildOrThrow());
            });
        }
    }

    private static void emitFace(BufferBuilder buf, int face, float alpha)
    {
        final float nx = FACE_NORMALS[face * 3];
        final float ny = FACE_NORMALS[face * 3 + 1];
        final float nz = FACE_NORMALS[face * 3 + 2];

        for (int v = 0; v < 4; v++)
        {
            final int i = (face * 4 + v) * 3;
            final float x = CubeGeometry.vert(i);
            final float y = CubeGeometry.vert(i + 1);
            final float z = CubeGeometry.vert(i + 2);
            final float u = (v == 1 || v == 2) ? 1f : 0f;
            final float w = (v == 2 || v == 3) ? 1f : 0f;

            buf.addVertex(x, y, z).setUv(u, w).setColor(1f, 1f, 1f, alpha).setNormal(nx, ny, nz);
        }
    }
}
