package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Core.Render.Helper.CubeGeometry;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.AtmosphereDefinition;
import cute.ame.pioneer.SkyPlanet.Physics.PlanetEnvironment;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;

public final class AtmosphereRenderer
{
    private static final ResourceLocation ATMOSPHERE_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "atmosphere");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(ATMOSPHERE_RENDER_TYPE);
    }

    private static final float MAX_CAM_DIST_OBJ = 64.0f;
    private static final float FRESNEL_POWER = 3.0f;
    private static final float MULTI_SCATTER = 0.6f;

    public static void render(PoseStack poseStack, AtmosphereDefinition atmo, PlanetEnvironment env, float radiusKm, float camDirX, float camDirY, float camDirZ, float sunDirX, float sunDirY, float sunDirZ, float camDistObj)
    {
        final float clampedCamDist = Math.min(camDistObj, MAX_CAM_DIST_OBJ);
        final float planetHalf = 1.0f;
        final double tempK = env.surfaceTempK();
        final double gravity = env.gravityMs2();
        final float shellScale = atmo.shellScale(tempK, gravity, radiusKm);
        final float rayleighH = atmo.rayleighScaleHeightFrac(tempK, gravity, radiusKm);
        final float mieH = atmo.mieScaleHeightFrac(tempK, gravity, radiusKm);
        final float opacity = atmo.opacity(gravity);
        final float shellThickness = Math.max((shellScale - 1.0f) * planetHalf, 1e-4f);
        final float atmoBoundRadius = planetHalf * 1.7320508f + shellThickness;

        final float[] rgb = atmo.colorRgb(gravity);
        final float r = rgb[0], g = rgb[1], b = rgb[2];

        final float sunIntensity = (float) (14.0 * env.irradianceRelative());
        final Matrix4f planetModel = new Matrix4f(poseStack.last().pose());

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        VeilSkyShaderHelper.draw(
        ATMOSPHERE_RENDER_TYPE,
        shader ->
        {
            set(shader, "uColor", r, g, b);
            set(shader, "uFresnelPower", FRESNEL_POWER);
            set(shader, "uOpacity", opacity);
            set(shader, "uCamDir", camDirX, camDirY, camDirZ);
            set(shader, "uSunDir", sunDirX, sunDirY, sunDirZ);
            set(shader, "uCamDist", clampedCamDist);
            set(shader, "uPlanetHalfExtent", planetHalf);
            set(shader, "uShellThickness", shellThickness);
            set(shader, "uAtmoBoundRadius", atmoBoundRadius);
            set(shader, "uRayleighScaleHeight", rayleighH);
            set(shader, "uMieScaleHeight", mieH);
            set(shader, "uMieG", atmo.mieG());
            set(shader, "uMieStrength", atmo.mieStrength());
            set(shader, "uSunIntensity", sunIntensity);
            set(shader, "uOzoneStrength", atmo.ozoneStrength());
            set(shader, "uMultiScatterStrength", MULTI_SCATTER);
            set(shader, "uPlanetModel", planetModel);
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            CubeGeometry.emit(buf, atmoBoundRadius * 2.0f);
            renderType.draw(buf.buildOrThrow());
        });

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }
}
