package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.JetConeDefinition;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public final class JetConeRenderer
{
    private static final ResourceLocation JET_FLARE_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "jet_flare");

    private static final float FLARE_TAPER_EXPONENT = 1.6f;
    private static final float RADIUS_SCALE = .9f;

    public static void render(PoseStack ps, JetConeDefinition jet, long tick, float partialTick, float apparentSize, MultiBufferSource.BufferSource bufferSource)
    {
        if (jet == null) return;

        float t = tick + partialTick;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        ps.pushPose();
        float precessAngle = (float) ((t / 20.0) * 2.0 * Math.PI * Math.max(jet.precessionSpeed(), 0f));
        ps.mulPose(new Quaternionf().rotationY(precessAngle));
        ps.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(jet.axialTilt())));

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        drawCore(ps, jet, t, apparentSize, 1f);
        drawCore(ps, jet, t, apparentSize, -1f);
        drawFlare(ps, jet, t, apparentSize, 1f, bufferSource);
        drawFlare(ps, jet, t, apparentSize, -1f, bufferSource);

        ps.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static final float CORE_ORBIT_FRACTION = 0.88f;
    private static final float CORE_THICKNESS = 1.35f;

    private static void drawCore(PoseStack ps, JetConeDefinition jet, float t, float apparentSize, float dir)
    {
        int segments = Math.max(jet.segments(), 3);
        int radial = 8;
        float length = jet.length() * apparentSize;
        float baseR = jet.baseRadius() * apparentSize;
        float tipR = jet.tipRadius() * apparentSize;
        float coreR = baseR * CORE_THICKNESS * RADIUS_SCALE;
        float wobbleSpeed = jet.wobbleSpeed();
        float helixTurns = jet.helixTurns();

        float r = lerp(jet.r(), 1f, 0.35f);
        float g = lerp(jet.g(), 1f, 0.35f);
        float b = lerp(jet.b(), 1f, 0.35f);
        float intensity = jet.intensity();

        float spinAngle = (float) ((t / 20.0) * 2.0 * Math.PI * wobbleSpeed);

        float[] cosA = new float[radial];
        float[] sinA = new float[radial];
        for (int j = 0; j < radial; j++)
        {
            double a = (2.0 * Math.PI * j) / radial;
            cosA[j] = (float) Math.cos(a);
            sinA[j] = (float) Math.sin(a);
        }

        float[] centerX = new float[segments + 1];
        float[] centerZ = new float[segments + 1];
        float[] ringY = new float[segments + 1];
        float[] tubeR = new float[segments + 1];
        float[] tubeA = new float[segments + 1];
        for (int i = 0; i <= segments; i++)
        {
            float f = (float) i / segments;
            float eased = (float) Math.pow(f, FLARE_TAPER_EXPONENT);
            float flareRadiusHere = baseR + (tipR - baseR) * eased;
            float helixAngle = dir * f * helixTurns * 2.0f * (float) Math.PI + spinAngle;
            float orbitR = flareRadiusHere * CORE_ORBIT_FRACTION * smoothstep(0f, 0.08f, f);

            centerX[i] = (float) Math.cos(helixAngle) * orbitR;
            centerZ[i] = (float) Math.sin(helixAngle) * orbitR;
            ringY[i] = dir * length * f;
            tubeR[i] = coreR * (1.0f - 0.35f * f);
            tubeA[i] = intensity * (1.0f - f * f) * smoothstep(0f, 0.03f, f);
        }

        Tesselator tess = Tesselator.getInstance();
        Matrix4f m = ps.last().pose();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < segments; i++)
        {
            for (int j = 0; j < radial; j++)
            {
                int jn = (j + 1) % radial;
                buf.addVertex(m, centerX[i] + cosA[j]  * tubeR[i],   ringY[i], centerZ[i] + sinA[j] * tubeR[i] ).setColor(r, g, b, tubeA[i]);
                buf.addVertex(m, centerX[i] + cosA[jn] * tubeR[i],   ringY[i], centerZ[i] + sinA[jn] * tubeR[i] ).setColor(r, g, b, tubeA[i]);
                buf.addVertex(m, centerX[i+1] + cosA[jn] * tubeR[i+1], ringY[i+1], centerZ[i+1] + sinA[jn] * tubeR[i+1]).setColor(r, g, b, tubeA[i+1]);
                buf.addVertex(m, centerX[i+1] + cosA[j]  * tubeR[i+1], ringY[i+1], centerZ[i+1] + sinA[j] * tubeR[i+1]).setColor(r, g, b, tubeA[i+1]);
            }
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    private static void drawFlare(PoseStack ps, JetConeDefinition jet, float t, float apparentSize, float dir, MultiBufferSource.BufferSource bufferSource)
    {
        int segments = Math.max(jet.segments(), 3);
        int radial = 20;
        float length = jet.length() * apparentSize;
        float baseR = jet.baseRadius() * apparentSize;
        float tipR = jet.tipRadius() * apparentSize;
        float wobbleSpeed = jet.wobbleSpeed();

        float fr = jet.r(), fg = jet.g(), fb = jet.b();
        float intensity = jet.intensity();

        float seconds = t / 20.0f;
        float[] ringRadius = new float[segments + 1];
        float[] ringBaseAlpha = new float[segments + 1];
        for (int i = 0; i <= segments; i++)
        {
            float f = (float) i / segments;
            float eased = (float) Math.pow(f, FLARE_TAPER_EXPONENT);

            float turbulence = 1.0f + 0.10f * sinN(f * 9.0f + t * 0.03f * wobbleSpeed) + 0.06f * sinN(f * 21.0f - t * 0.05f * wobbleSpeed + dir * 3.1f);
            ringRadius[i] = (baseR + (tipR - baseR) * eased) * turbulence * RADIUS_SCALE;

            float fadeOut = (float) Math.pow(1.0f - f, 0.4f);
            float fadeIn = smoothstep(0f, 0.04f, f);
            ringBaseAlpha[i] = intensity * 0.9f * fadeOut * fadeIn;
        }

        float[] cosA = new float[radial + 1];
        float[] sinA = new float[radial + 1];
        float[] angleNorm = new float[radial + 1];
        for (int j = 0; j <= radial; j++)
        {
            double a = (j == radial) ? 0.0 : (2.0 * Math.PI * j) / radial;
            cosA[j] = (float) Math.cos(a);
            sinA[j] = (float) Math.sin(a);
            angleNorm[j] = (float) j / radial;
        }

        cute.ame.auralithpioneerinitiative.Core.Compat.VeilSkyShaderHelper.draw(
        JET_FLARE_RENDER_TYPE,
        shader ->
        {
            setUniform(shader, "uTime", seconds);
            setUniform(shader, "uDir", dir);
            setUniform(shader, "uHelixTurns", jet.helixTurns());
            setUniform(shader, "uFiberDistortion", jet.fiberDistortion());
            setUniform(shader, "uTrailPersistence", jet.trailPersistence());
            setUniform(shader, "uWobbleSpeed", wobbleSpeed);
        },
        renderType ->
        {
            Tesselator tess = Tesselator.getInstance();
            Matrix4f m = ps.last().pose();
            BufferBuilder buf = tess.begin(renderType.mode(), renderType.format());

            for (int i = 0; i < segments; i++)
            {
                float y0 = dir * length * ((float) i / segments);
                float y1 = dir * length * ((float) (i + 1) / segments);
                float rad0 = ringRadius[i], rad1 = ringRadius[i + 1];
                float f0 = (float) i / segments;
                float f1 = (float) (i + 1) / segments;
                float a0 = ringBaseAlpha[i], a1 = ringBaseAlpha[i + 1];

                for (int j = 0; j < radial; j++)
                {
                    int jn = j + 1;
                    buf.addVertex(m, cosA[j]  * rad0, y0, sinA[j]  * rad0).setColor(fr, fg, fb, a0).setUv(f0, angleNorm[j]).setLight(15728880);
                    buf.addVertex(m, cosA[jn] * rad0, y0, sinA[jn] * rad0).setColor(fr, fg, fb, a0).setUv(f0, angleNorm[jn]).setLight(15728880);
                    buf.addVertex(m, cosA[jn] * rad1, y1, sinA[jn] * rad1).setColor(fr, fg, fb, a1).setUv(f1, angleNorm[jn]).setLight(15728880);
                    buf.addVertex(m, cosA[j]  * rad1, y1, sinA[j]  * rad1).setColor(fr, fg, fb, a1).setUv(f1, angleNorm[j]).setLight(15728880);
                }
            }

            renderType.draw(buf.buildOrThrow());
        });
    }

    private static void setUniform(ShaderProgram shader, String name, float value)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setFloat(value);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float smoothstep(float edge0, float edge1, float x)
    {
        float tt = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return tt * tt * (3.0f - 2.0f * tt);
    }

    private static float sinN(float x)
    {
        return (float) (Math.sin(x) * 0.5 + 0.5);
    }
}