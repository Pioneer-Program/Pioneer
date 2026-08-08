package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.auralithpioneerinitiative.SkyPlanet.RenderingHelper.ShaderHelper;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.CelestialMath;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.CubemapTextures;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class CubeMeshRenderer
{
    public static final float NO_RINGS = -1.0f;
    private static final float NIGHT_FACE_FLOOR = 0.03f;
    private static final float FACE_TERMINATOR_SOFTNESS = 0.35f;
    private static final float RING_SHADOW_SOFTNESS_FRAC = 0.06f;

    public static void renderTextureCube(PoseStack ps, CubemapTextures cubemap, boolean cullEnabled)
    {
        renderTextureCubeShaded(ps, cubemap, cullEnabled, 0f, 0f, 0f, 1f, NO_RINGS, NO_RINGS);
    }

    public static void renderTextureCubeShaded(PoseStack ps, CubemapTextures cubemap, boolean cullEnabled, float sunDirX, float sunDirY, float sunDirZ, float alpha, float ringInnerR, float ringOuterR)
    {
        if (ShaderHelper.shadersActive()) RenderSystem.setShader(GameRenderer::getRendertypeEntitySolidShader);
        else RenderSystem.setShader(GameRenderer::getPositionTexShader);

        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        if (cullEnabled) RenderSystem.enableCull();
        else RenderSystem.disableCull();

        Matrix4f m = ps.last().pose();
        float h = 0.5f;
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        boolean shaders = ShaderHelper.shadersActive();
        boolean shaded = (sunDirX != 0f || sunDirY != 0f || sunDirZ != 0f);
        boolean ringShadowEnabled = ringInnerR > 0f && ringOuterR > ringInnerR;

        face(ps, m, cubemap.top(), shaders, light, overlay, -h, h, h,  h, h, h,  h, h,-h, -h, h,-h, 0, 1, 0, shaded, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR, alpha);
        face(ps, m, cubemap.bottom(), shaders, light, overlay, -h,-h,-h,  h,-h,-h,  h,-h, h, -h,-h, h, 0,-1, 0, shaded, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR, alpha);
        face(ps, m, cubemap.front(), shaders, light, overlay,  h, h, h, -h, h, h, -h,-h, h,  h,-h, h, 0, 0, 1, shaded, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR, alpha);
        face(ps, m, cubemap.back(), shaders, light, overlay, -h, h,-h,  h, h,-h,  h,-h,-h, -h,-h,-h, 0, 0,-1, shaded, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR, alpha);
        face(ps, m, cubemap.right(), shaders, light, overlay,  h, h,-h,  h, h, h,  h,-h, h,  h,-h,-h, 1, 0, 0, shaded, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR, alpha);
        face(ps, m, cubemap.left(), shaders, light, overlay, -h, h, h, -h, h,-h, -h,-h,-h, -h,-h, h, -1, 0, 0, shaded, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR, alpha);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
    }

    private static float vertexShade(boolean shaded, float x, float y, float z, float nx, float ny, float nz, float sunDirX, float sunDirY, float sunDirZ, boolean ringShadowEnabled, float ringInnerR, float ringOuterR)
    {
        if (!shaded) return 1.0f;

        float lit = dayNightLit(nx, ny, nz, sunDirX, sunDirY, sunDirZ);
        if (ringShadowEnabled) lit *= ringShadowLit(x, y, z, sunDirX, sunDirY, sunDirZ, ringInnerR, ringOuterR);

        return NIGHT_FACE_FLOOR + (1.0f - NIGHT_FACE_FLOOR) * lit;
    }

    private static float dayNightLit(float nx, float ny, float nz, float sunDirX, float sunDirY, float sunDirZ)
    {
        float ndots = nx * sunDirX + ny * sunDirY + nz * sunDirZ;

        return smoothstep(-FACE_TERMINATOR_SOFTNESS, FACE_TERMINATOR_SOFTNESS, ndots);
    }

    private static float ringShadowLit(float x, float y, float z, float sunDirX, float sunDirY, float sunDirZ, float ringInnerR, float ringOuterR)
    {
        if (Math.abs(sunDirY) < 1e-6f) return 1.0f;

        float t = -y / sunDirY;
        if (t <= 0f) return 1.0f;

        float ix = x + t * sunDirX;
        float iz = z + t * sunDirZ;
        float radial = (float) Math.sqrt(ix * ix + iz * iz);

        float soft = Math.max((ringOuterR - ringInnerR) * RING_SHADOW_SOFTNESS_FRAC, 1e-4f);
        float pastInner = smoothstep(ringInnerR - soft, ringInnerR + soft, radial);
        float pastOuter = smoothstep(ringOuterR - soft, ringOuterR + soft, radial);

        float shadowedAmount = pastInner * (1.0f - pastOuter);
        return 1.0f - shadowedAmount;
    }

    private static float smoothstep(float edge0, float edge1, float x)
    {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0f - 2.0f * t);
    }

    private static float clamp01(float v)
    {
        return v < 0f ? 0f : (Math.min(v, 1f));
    }

    private static void face(PoseStack ps, Matrix4f m, ResourceLocation tex, boolean shaders, int light, int overlay, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, boolean shaded, float sunDirX, float sunDirY, float sunDirZ, boolean ringShadowEnabled, float ringInnerR, float ringOuterR, float alpha)
    {
        float s0 = vertexShade(shaded, x0, y0, z0, nx, ny, nz, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR);
        float s1 = vertexShade(shaded, x1, y1, z1, nx, ny, nz, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR);
        float s2 = vertexShade(shaded, x2, y2, z2, nx, ny, nz, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR);
        float s3 = vertexShade(shaded, x3, y3, z3, nx, ny, nz, sunDirX, sunDirY, sunDirZ, ringShadowEnabled, ringInnerR, ringOuterR);
        drawCubeFace(ps, m, tex, shaders, light, overlay, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz, s0, s1, s2, s3, alpha);
    }

    private static void drawCubeFace(PoseStack ps, Matrix4f m, ResourceLocation tex, boolean shaders, int light, int overlay, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float nx, float ny, float nz, float shade0, float shade1, float shade2, float shade3, float alpha)
    {
        RenderSystem.setShaderTexture(0, tex);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = shaders ? tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY) : tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        if (shaders)
        {
            buf.addVertex(m, x0, y0, z0).setColor(shade0,shade0,shade0,alpha).setUv(0,0).setOverlay(overlay).setLight(light).setNormal(ps.last(), nx, ny, nz);
            buf.addVertex(m, x1, y1, z1).setColor(shade1,shade1,shade1,alpha).setUv(1,0).setOverlay(overlay).setLight(light).setNormal(ps.last(), nx, ny, nz);
            buf.addVertex(m, x2, y2, z2).setColor(shade2,shade2,shade2,alpha).setUv(1,1).setOverlay(overlay).setLight(light).setNormal(ps.last(), nx, ny, nz);
            buf.addVertex(m, x3, y3, z3).setColor(shade3,shade3,shade3,alpha).setUv(0,1).setOverlay(overlay).setLight(light).setNormal(ps.last(), nx, ny, nz);
        }
        else
        {
            float avg = (shade0 + shade1 + shade2 + shade3) * 0.25f;
            RenderSystem.setShaderColor(avg, avg, avg, alpha);
            buf.addVertex(m, x0, y0, z0).setUv(0,0);
            buf.addVertex(m, x1, y1, z1).setUv(1,0);
            buf.addVertex(m, x2, y2, z2).setUv(1,1);
            buf.addVertex(m, x3, y3, z3).setUv(0,1);
        }

        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    public static void renderColoredCube(PoseStack ps, float r, float g, float b, float a)
    {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = ps.last().pose();
        for (float[][] face : CelestialMath.CUBE_FACES)
            for (float[] v : face)
                buf.addVertex(m, v[0]*0.5f, v[1]*0.5f, v[2]*0.5f).setColor(r, g, b, a);

        BufferUploader.drawWithShader(buf.buildOrThrow());
    }
}