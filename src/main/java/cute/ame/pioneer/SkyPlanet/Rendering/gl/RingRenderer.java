package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.SkyPlanet.Data.RingDefinition;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import cute.ame.pioneer.Core.Render.Debug.ShadingDebugMode;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;
import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.setInt;

public final class RingRenderer
{
    private static final ResourceLocation RINGS = ResourceLocation.fromNamespaceAndPath("pioneer", "rings");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(RINGS);
    }

    private static final float BOUND_HEIGHT_FRAC = 0.05f;
    private static final float EXPOSURE = 3.0f;
    private static final float SHINE_R = 0.95f, SHINE_G = 0.85f, SHINE_B = 0.70f;

    public static void render(PoseStack ps, RingDefinition rings, float apparentSize, float camObjX, float camObjY, float camObjZ, float sunX, float sunY, float sunZ, float sunAngRad)
    {
        final float planetHalf = apparentSize * 0.5f;
        final float innerR = rings.innerRadius() * planetHalf;
        final float outerR = rings.outerRadius() * planetHalf;
        if (outerR <= innerR) return;

        final float boundXZ = outerR * 2.0f * (1.0f + 0.4143f * rings.squareness());
        final float boundY = Math.max(outerR * BOUND_HEIGHT_FRAC, planetHalf * 0.05f) * 2.0f;

        final Matrix4f model = new Matrix4f(ps.last().pose());

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        VeilSkyShaderHelper.draw(
        RINGS,
        shader ->
        {
            set(shader, "uPlanetModel", model);
            set(shader, "uCamPos", camObjX, camObjY, camObjZ);
            set(shader, "uSunDir", sunX, sunY, sunZ);

            set(shader, "uPlanetHalf", planetHalf);
            set(shader, "uInnerR", innerR);
            set(shader, "uOuterR", outerR);
            set(shader, "uSquareness", rings.squareness());

            set(shader, "uColorMin", rings.minR(), rings.minG(), rings.minB());
            set(shader, "uColorMax", rings.maxR(), rings.maxG(), rings.maxB());
            set(shader, "uOpacity", rings.opacity());
            set(shader, "uBandScale", rings.bandScale());
            set(shader, "uBandContrast", rings.bandContrast());
            set(shader, "uGapStrength", rings.gapStrength());
            set(shader, "uSeed", (float) (rings.seed() & 0xFFFFL));

            set(shader, "uSunAngRad", Math.max(sunAngRad, 1e-6f));
            set(shader, "uSSA", rings.singleScatterAlbedo());
            set(shader, "uAsymmetry", rings.asymmetry());
            set(shader, "uBackFrac", rings.backscatterFraction());
            set(shader, "uOppB0", rings.oppositionB0());
            set(shader, "uOppH", rings.oppositionH());
            set(shader, "uPlanetShine", rings.planetShine());
            set(shader, "uPlanetTint", SHINE_R, SHINE_G, SHINE_B);
            set(shader, "uExposure", EXPOSURE);
            setInt(shader, "uDebug", ShadingDebugMode.currentShaderId());
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            emitBox(buf, boundXZ, boundY);
            renderType.draw(buf.buildOrThrow());
        });

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    private static void emitBox(BufferBuilder buf, float sizeXZ, float sizeY)
    {
        final float hx = sizeXZ * 0.5f, hy = sizeY * 0.5f, hz = sizeXZ * 0.5f;
        quad(buf,  hx,-hy,-hz,  hx,-hy, hz,  hx, hy, hz,  hx, hy,-hz,  1, 0, 0);
        quad(buf, -hx,-hy, hz, -hx,-hy,-hz, -hx, hy,-hz, -hx, hy, hz, -1, 0, 0);
        quad(buf, -hx, hy,-hz,  hx, hy,-hz,  hx, hy, hz, -hx, hy, hz,  0, 1, 0);
        quad(buf, -hx,-hy, hz,  hx,-hy, hz,  hx,-hy,-hz, -hx,-hy,-hz,  0,-1, 0);
        quad(buf, -hx,-hy, hz, -hx, hy, hz,  hx, hy, hz,  hx,-hy, hz,  0, 0, 1);
        quad(buf,  hx,-hy,-hz,  hx, hy,-hz, -hx, hy,-hz, -hx,-hy,-hz,  0, 0,-1);
    }

    private static void quad(BufferBuilder buf, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz)
    {
        buf.addVertex(ax, ay, az).setColor(1f, 1f, 1f, 1f).setNormal(nx, ny, nz);
        buf.addVertex(bx, by, bz).setColor(1f, 1f, 1f, 1f).setNormal(nx, ny, nz);
        buf.addVertex(cx, cy, cz).setColor(1f, 1f, 1f, 1f).setNormal(nx, ny, nz);
        buf.addVertex(dx, dy, dz).setColor(1f, 1f, 1f, 1f).setNormal(nx, ny, nz);
    }
}
