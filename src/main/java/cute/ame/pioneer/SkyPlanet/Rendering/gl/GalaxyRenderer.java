package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Core.Render.Baking.LUT.BuiltinLUTs;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTParams;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTRegistry;
import cute.ame.pioneer.Core.Render.Helper.CubeGeometry;
import cute.ame.pioneer.Core.Render.Helper.SamplerBinder;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;

public final class GalaxyRenderer
{
    private static final ResourceLocation GALAXY_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "galaxy");

    private static final int  UNIT_SKYBOX_LUT = 4;
    private static final long GALAXY_SEED = 0x6A1ACC1A0L;
    private static final float SKYBOX_CUBE_SCALE = 2.0f;

    static
    {
        VeilSkyShaderHelper.registerVfxShader(GALAXY_RENDER_TYPE);
    }

    public static void render(PoseStack poseStack, Matrix4f projMat, long tick, float partialTick)
    {
        float time = (tick + partialTick) / 20.0f;
        Matrix4f viewRot = new Matrix4f(poseStack.last().pose());

        Matrix4f viewProj = new Matrix4f(projMat).mul(viewRot);
        Matrix4f invViewProj = new Matrix4f(viewProj).invert();

        com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
        float screenW = window.getWidth();
        float screenH = window.getHeight();

        RenderSystem.disableCull();

        final ResourceLocation skyboxLut = LUTRegistry.get(BuiltinLUTs.GALAXY_SKYBOX, LUTParams.of(GALAXY_SEED, BuiltinLUTs.GALAXY_SKYBOX_RES));

        VeilSkyShaderHelper.draw(
        GALAXY_RENDER_TYPE,
        shader ->
        {
            SamplerBinder.bind(shader, "uSkyboxLUT", skyboxLut, UNIT_SKYBOX_LUT);

            set(shader, "uTime", time);
            set(shader, "uInvViewProj", invViewProj);
            set(shader, "uScreenWidth", screenW);
            set(shader, "uScreenHeight", screenH);
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            CubeGeometry.emit(buf, SKYBOX_CUBE_SCALE);
            renderType.draw(buf.buildOrThrow());
        });

        RenderSystem.enableCull();
    }
}
