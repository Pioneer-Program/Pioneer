package cute.ame.pioneer.SkyPlanet.Rendering.gl;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Core.Render.Helper.CubeGeometry;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.Core.Render.Helper.SamplerBinder;
import cute.ame.pioneer.SkyPlanet.Data.RingDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import javax.annotation.Nullable;
import cute.ame.pioneer.Core.Render.Debug.ShadingDebugMode;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;
import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.setInt;
public final class BodyRenderer
{
    private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath("pioneer", "body");
    static
    {
        VeilSkyShaderHelper.registerVfxShader(BODY);
    }
    
    private static final float NIGHT_FLOOR = 0.03f;
    private static final float ATMO_TERMINATOR = 0.35f;
    private static final float HALF = 0.5f;
    private static final float CURVATURE = 0.45f;
    private static final float SCATTER_WIDTH = 0.35f;
    private static final float SCATTER_STRENGTH = 0.35f;
    private static final float SCATTER_R = 1.00f, SCATTER_G = 0.62f, SCATTER_B = 0.36f;

    public static void render(PoseStack ps, CubemapTextures cubemap, float alpha, float camObjX, float camObjY, float camObjZ, float sunX, float sunY, float sunZ, @Nullable RingDefinition rings, float sunAngRad, boolean hasAtmosphere)
    {
        final Matrix4f model = new Matrix4f(ps.last().pose());
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        VeilSkyShaderHelper.draw(
        BODY,
        shader ->
        {
            set(shader, "uPlanetModel", model);
            set(shader, "uCamPos", camObjX, camObjY, camObjZ);
            set(shader, "uSunDir", sunX, sunY, sunZ);
            set(shader, "uHalf", HALF);
            set(shader, "uAlpha", alpha);
            set(shader, "uNightFloor", NIGHT_FLOOR);
            set(shader, "uTerminator", Math.max(sunAngRad, hasAtmosphere ? ATMO_TERMINATOR : 0.02f));
            set(shader, "uSunAngRad", Math.max(sunAngRad, 1e-5f));
            setInt(shader, "uDebug", ShadingDebugMode.currentShaderId());
            set(shader, "uCurvature", CURVATURE);
            set(shader, "uScatterWidth", SCATTER_WIDTH);
            set(shader, "uScatterStrength", SCATTER_STRENGTH);
            set(shader, "uScatterColor", SCATTER_R, SCATTER_G, SCATTER_B);
            if (rings != null)
            {
                set(shader, "uRingInner", rings.innerRadius() * HALF);
                set(shader, "uRingOuter", rings.outerRadius() * HALF);
                set(shader, "uRingSquareness", rings.squareness());
                set(shader, "uRingBandScale", rings.bandScale());
                set(shader, "uRingBandContrast", rings.bandContrast());
                set(shader, "uRingGapStrength", rings.gapStrength());
                set(shader, "uRingOpacity", rings.opacity());
                set(shader, "uRingSeed", (float) (rings.seed() & 0xFFFFL));
            }
            else
            {
                set(shader, "uRingOuter", -1.0f);
            }
            SamplerBinder.bindNearest(shader, "uFaceFront", cubemap.front(), 0);
            SamplerBinder.bindNearest(shader, "uFaceBack", cubemap.back(), 1);
            SamplerBinder.bindNearest(shader, "uFaceLeft", cubemap.left(), 2);
            SamplerBinder.bindNearest(shader, "uFaceRight", cubemap.right(), 3);
            SamplerBinder.bindNearest(shader, "uFaceTop", cubemap.top(), 4);
            SamplerBinder.bindNearest(shader, "uFaceBottom", cubemap.bottom(), 5);
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