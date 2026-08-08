package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.auralithpioneerinitiative.Core.Compat.VeilSkyShaderHelper;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.CloudShadowParams;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.CubeGeometry;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.SamplerBinder;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.Bakers.CloudVolumeBaker;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.BuiltinLUTs;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTParams;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTRegistry;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.CloudsDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import static cute.ame.auralithpioneerinitiative.Core.Render.Helper.UniformHelper.set;
import static cute.ame.auralithpioneerinitiative.Core.Render.Helper.UniformHelper.setInt;

public final class CloudsRenderer
{
    private static final ResourceLocation CLOUDS_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "clouds");

    static { VeilSkyShaderHelper.registerVfxShader(CLOUDS_RENDER_TYPE); }

    private static final float MAX_CAM_DIST_OBJ = 64.0f;

    private static final int MAX_VIEW_STEPS = 16;
    private static final int MAX_SUN_STEPS  = 3;
    private static final int MIN_VIEW_STEPS = 6;

    private static final float TIME_SCALE = 0.035f;
    private static final float BASE_FREQ_MULT = 3.2f;
    private static final float WEATHER_DRIFT = 0.006f;
    private static final float COVERAGE_GAIN = 1.4f;
    private static final float SUN_MARCH_SCALE = 2.0f;

    private static final float VOLUME_FREQ_SCALE = 1.0f / CloudVolumeBaker.BASE_FREQ;
    private static final long NOISE_SEED = 0xCC45A4f34L;

    private static final int UNIT_NOISE_VOLUME = 4;
    private static final int UNIT_WEATHER_LUT  = 5;

    public static void render(PoseStack poseStack, CloudsDefinition clouds, float camDirX, float camDirY, float camDirZ, float sunDirX, float sunDirY, float sunDirZ, float camDistObj, float timeSeconds)
    {
        final float clampedCamDist = Math.min(camDistObj, MAX_CAM_DIST_OBJ);
        final float planetHalf = 0.5f;
        final float cloudInner = Math.max(clouds.innerAltitude(), 1e-4f);
        final float cloudOuter = Math.max(clouds.outerAltitude(), cloudInner + 1e-4f);
        final float cloudBoundRadius = planetHalf * 1.7320508f + cloudOuter;

        final float shellThickness = Math.max(cloudOuter - cloudInner, 1e-4f);
        final float invShellThickness = 1.0f / shellThickness;
        final float extinction = 6.0f / shellThickness;
        final float maxSegmentLen = shellThickness * 12.0f;

        final float windT = timeSeconds * TIME_SCALE * clouds.windSpeed();
        final float windOX = clouds.windX() * windT;
        final float windOZ = clouds.windZ() * windT;

        final float driftAngle = windT * WEATHER_DRIFT;
        final float driftSin = (float) Math.sin(driftAngle);
        final float driftCos = (float) Math.cos(driftAngle);

        final float volumeFreq = Math.max(clouds.noiseScale() * BASE_FREQ_MULT, 0.01f) * VOLUME_FREQ_SCALE;
        final float coverageBias = (clouds.coverage() - 0.5f) * COVERAGE_GAIN;

        final float apparent = cloudBoundRadius / Math.max(clampedCamDist, 1e-3f);
        final int viewSteps = Math.clamp(Math.round(MIN_VIEW_STEPS + apparent * 48.0f), MIN_VIEW_STEPS, MAX_VIEW_STEPS);
        final int sunSteps = viewSteps >= 12 ? MAX_SUN_STEPS : 2;

        final float sunMarchLen = cloudOuter * SUN_MARCH_SCALE;
        final float invSunSteps = 1.0f / sunSteps;

        final ResourceLocation volumeLut = LUTRegistry.get(BuiltinLUTs.CLOUD_VOLUME, LUTParams.of(NOISE_SEED, BuiltinLUTs.CLOUD_VOLUME_RES));
        final ResourceLocation weatherLut = LUTRegistry.get(BuiltinLUTs.CLOUD_WEATHER, LUTParams.of(NOISE_SEED, BuiltinLUTs.CLOUD_WEATHER_RES));

        final float r = clouds.r(), g = clouds.g(), b = clouds.b();
        final Matrix4f planetModel = new Matrix4f(poseStack.last().pose());

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        VeilSkyShaderHelper.draw(
        CLOUDS_RENDER_TYPE,
        shader ->
        {
            SamplerBinder.bind(shader, "uNoiseVolumeLUT", volumeLut,  UNIT_NOISE_VOLUME);
            SamplerBinder.bind(shader, "uWeatherLUT", weatherLut, UNIT_WEATHER_LUT);

            set(shader, "uColor", r, g, b);
            set(shader, "uCamDir", camDirX, camDirY, camDirZ);
            set(shader, "uSunDir", sunDirX, sunDirY, sunDirZ);
            set(shader, "uCamDist", clampedCamDist);

            set(shader, "uPlanetHalfExtent", planetHalf);
            set(shader, "uCloudInner", cloudInner);
            set(shader, "uCloudOuter", cloudOuter);
            set(shader, "uAtmoBoundRadius", cloudBoundRadius);

            set(shader, "uCoverage", clouds.coverage());
            set(shader, "uCoverageBias", coverageBias);
            set(shader, "uDensity", clouds.density());
            set(shader, "uErosion", clouds.erosion());

            set(shader, "uWindOffset", windOX, 0.0f, windOZ);
            set(shader, "uWeatherDriftSC", driftSin, driftCos);
            set(shader, "uVolumeFreq", volumeFreq);
            set(shader, "uInvShellThickness", invShellThickness);
            set(shader, "uExtinction", extinction);
            set(shader, "uMaxSegmentLen", maxSegmentLen);
            set(shader, "uSunMarchLen", sunMarchLen);
            set(shader, "uInvSunSteps", invSunSteps);

            set(shader, "uMieG", clouds.mieG());
            set(shader, "uSunIntensity", clouds.sunIntensity());
            set(shader, "uMultiScatterStrength", clouds.multiScatterStrength());
            set(shader, "uPowderStrength", clouds.powderStrength());

            setInt(shader, "uViewSteps", viewSteps);
            setInt(shader, "uSunSteps", sunSteps);

            set(shader, "uPlanetModel", planetModel);
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            CubeGeometry.emit(buf, cloudBoundRadius * 2.0f);
            renderType.draw(buf.buildOrThrow());
        });

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        CloudShadowParams.publish(weatherLut, coverageBias, driftSin, driftCos, cloudInner, 1.0f);
    }
}
