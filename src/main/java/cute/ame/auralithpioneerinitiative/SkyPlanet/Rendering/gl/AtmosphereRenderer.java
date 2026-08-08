package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.auralithpioneerinitiative.Core.Compat.VeilSkyShaderHelper;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.CubeGeometry;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.AtmosphereDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import static cute.ame.auralithpioneerinitiative.Core.Render.Helper.UniformHelper.set;

public final class AtmosphereRenderer
{
    private static final ResourceLocation ATMOSPHERE_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "atmosphere");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(ATMOSPHERE_RENDER_TYPE);
    }

    private static final float MAX_CAM_DIST_OBJ = 64.0f;

    public static void render(PoseStack poseStack, AtmosphereDefinition atmo, float camDirX, float camDirY, float camDirZ, float sunDirX, float sunDirY, float sunDirZ, float camDistObj)
    {
        final float clampedCamDist = Math.min(camDistObj, MAX_CAM_DIST_OBJ);
        final float planetHalf = 0.5f;
        final float shellThickness = Math.max((atmo.scale() - 1.0f) * planetHalf, 1e-4f);
        final float atmoBoundRadius = planetHalf * 1.7320508f + shellThickness;

        final float r = atmo.r(), g = atmo.g(), b = atmo.b();
        final Matrix4f planetModel = new Matrix4f(poseStack.last().pose());

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        VeilSkyShaderHelper.draw(
        ATMOSPHERE_RENDER_TYPE,
        shader ->
        {
            set(shader, "uColor", r, g, b);
            set(shader, "uFresnelPower", atmo.fresnelPower());
            set(shader, "uOpacity", atmo.opacity());
            set(shader, "uCamDir", camDirX, camDirY, camDirZ);
            set(shader, "uSunDir", sunDirX, sunDirY, sunDirZ);
            set(shader, "uCamDist", clampedCamDist);
            set(shader, "uPlanetHalfExtent", planetHalf);
            set(shader, "uShellThickness", shellThickness);
            set(shader, "uAtmoBoundRadius", atmoBoundRadius);
            set(shader, "uRayleighScaleHeight", atmo.rayleighScaleHeight());
            set(shader, "uMieScaleHeight", atmo.mieScaleHeight());
            set(shader, "uMieG", atmo.mieG());
            set(shader, "uMieStrength", atmo.mieStrength());
            set(shader, "uSunIntensity", atmo.sunIntensity());
            set(shader, "uOzoneStrength", atmo.ozoneStrength());
            set(shader, "uMultiScatterStrength", atmo.multiScatterStrength());
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
