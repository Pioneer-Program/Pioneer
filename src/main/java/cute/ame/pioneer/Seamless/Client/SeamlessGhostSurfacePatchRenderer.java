package cute.ame.pioneer.Seamless.Client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Seamless.SeamlessLevelRegistry;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.Optional;

public final class SeamlessGhostSurfacePatchRenderer
{
    private record FaceBasis(Vector3f normal, Vector3f tangent, Vector3f bitangent) {}
    private static final FaceBasis FACE_POS_X = new FaceBasis(new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));
    private static final FaceBasis FACE_NEG_X = new FaceBasis(new Vector3f(-1, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0));
    private static final FaceBasis FACE_POS_Y = new FaceBasis(new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1));
    private static final FaceBasis FACE_NEG_Y = new FaceBasis(new Vector3f(0, -1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1));
    private static final FaceBasis FACE_POS_Z = new FaceBasis(new Vector3f(0, 0, 1), new Vector3f(1, 0, 0), new Vector3f(0, -1, 0));
    private static final FaceBasis FACE_NEG_Z = new FaceBasis(new Vector3f(0, 0, -1), new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0));
    private static final float SURFACE_OFFSET = 0.001f;

    private static FaceBasis pickNearestFace(float dirX, float dirY, float dirZ)
    {
        float ax = Math.abs(dirX), ay = Math.abs(dirY), az = Math.abs(dirZ);
        if (ax >= ay && ax >= az) return dirX >= 0 ? FACE_POS_X : FACE_NEG_X;
        if (ay >= ax && ay >= az) return dirY >= 0 ? FACE_POS_Y : FACE_NEG_Y;
        return dirZ >= 0 ? FACE_POS_Z : FACE_NEG_Z;
    }

    public static void renderPatch(PoseStack ps, PlanetDefinition planet, float camLX, float camLY, float camLZ)
    {
        Optional<ResourceLocation> dimLoc = planet.dimension();
        if (dimLoc.isEmpty()) return;

        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimLoc.get());

        BlockPos anchor = SeamlessGhostAnchors.get(dimension);
        if (anchor == null) return;

        Optional<ClientLevel> ghostOpt = SeamlessLevelRegistry.get(dimension);
        if (ghostOpt.isEmpty()) return;
        ClientLevel ghost = ghostOpt.get();

        Map<Long, SeamlessGhostSectionMesher.MeshedSection> sections = SeamlessGhostSectionRenderer.sectionsFor(dimension);
        if (sections == null || sections.isEmpty()) return;

        FaceBasis face = pickNearestFace(camLX, camLY, camLZ);

        float worldRadius = Math.max(16, Config.SURFACE_PATCH_WORLD_RADIUS.get());
        float blockToUnit = 0.5f / worldRadius;
        int surfaceY = Config.SURFACE_PATCH_SEA_LEVEL.get() + 1;
        Pioneer.LOGGER.debug("[Auralith] Surface patch for {}: anchor={} surfaceY={} nearestFaceNormal=({},{},{}) sections={}", dimension.location(), anchor.toShortString(), surfaceY, face.normal().x, face.normal().y, face.normal().z, sections.size());

        ps.pushPose();
        ps.translate(face.normal().x * 0.5f + face.normal().x * SURFACE_OFFSET, face.normal().y * 0.5f + face.normal().y * SURFACE_OFFSET, face.normal().z * 0.5f + face.normal().z * SURFACE_OFFSET);
        Matrix4f basisRotation = new Matrix4f(face.tangent().x, face.tangent().y, face.tangent().z, 0f, face.normal().x, face.normal().y, face.normal().z, 0f, face.bitangent().x, face.bitangent().y, face.bitangent().z, 0f, 0f, 0f, 0f, 1f);
        ps.mulPose(basisRotation);
        ps.scale(blockToUnit, blockToUnit, blockToUnit);
        ps.translate(-anchor.getX(), -surfaceY, -anchor.getZ());

        drawSections(ps, sections, dimension, anchor, surfaceY);
        ps.popPose();
    }

    private static void drawSections(PoseStack ps, Map<Long, SeamlessGhostSectionMesher.MeshedSection> sections, ResourceKey<Level> dimension, BlockPos anchor, int surfaceY)
    {
        int renderRadius = SeamlessGhostSectionRenderer.renderRadiusSections();
        int sax = anchor.getX() >> 4, saz = anchor.getZ() >> 4, say = surfaceY >> 4;

        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        Matrix4f modelView = ps.last().pose();
        Matrix4f projection = RenderSystem.getProjectionMatrix();

        for (Map.Entry<Long, SeamlessGhostSectionMesher.MeshedSection> entry : sections.entrySet())
        {
            long key = entry.getKey();
            int sx = SectionPos.x(key), sy = SectionPos.y(key), sz = SectionPos.z(key);
            if (Math.abs(sx - sax) > renderRadius || Math.abs(sz - saz) > renderRadius || Math.abs(sy - say) > renderRadius) continue;

            VertexBuffer buffer = SeamlessGhostSectionGpuStore.getOrUpload(dimension, key, entry.getValue());
            if (buffer == null) continue;

            buffer.bind();
            buffer.drawWithShader(modelView, projection, RenderSystem.getShader());
            VertexBuffer.unbind();
        }

        mc.gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }
}