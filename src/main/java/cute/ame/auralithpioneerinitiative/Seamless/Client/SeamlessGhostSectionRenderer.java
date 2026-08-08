package cute.ame.auralithpioneerinitiative.Seamless.Client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SeamlessGhostSectionRenderer
{
    private static final Map<ResourceKey<Level>, Map<Long, SeamlessGhostSectionMesher.MeshedSection>> SECTIONS = new ConcurrentHashMap<>();

    private static final int RENDER_RADIUS_SECTIONS = 6;

    public static boolean isWithinRenderMargin(ResourceKey<Level> dimension, ChunkPos chunkPos)
    {
        BlockPos anchor = SeamlessGhostAnchors.get(dimension);
        if (anchor == null) return true;
        int cx = anchor.getX() >> 4, cz = anchor.getZ() >> 4;
        int margin = RENDER_RADIUS_SECTIONS + 1;
        return Math.abs(chunkPos.x - cx) <= margin && Math.abs(chunkPos.z - cz) <= margin;
    }

    public static void meshChunk(ClientLevel ghost, ResourceKey<Level> dimension, ChunkPos chunkPos)
    {
        if (!isWithinRenderMargin(dimension, chunkPos))
        {
            return;
        }

        Map<Long, SeamlessGhostSectionMesher.MeshedSection> forDim = SECTIONS.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>());

        var chunk = ghost.getChunk(chunkPos.x, chunkPos.z);
        int minSection = ghost.getMinSection();
        int maxSection = ghost.getMaxSection();

        BlockPos anchor = SeamlessGhostAnchors.get(dimension);
        Integer anchorSectionY = anchor != null ? (ghost.getHeight(Heightmap.Types.WORLD_SURFACE, anchor.getX(), anchor.getZ()) >> 4) : null;

        for (int sy = minSection; sy < maxSection; sy++)
        {
            if (anchorSectionY != null && Math.abs(sy - anchorSectionY) > RENDER_RADIUS_SECTIONS + 1) continue;

            try
            {
                if (chunk.getSection(sy - minSection).hasOnlyAir()) continue;
            }
            catch (Exception ignored)
            {
                continue;
            }

            BlockPos sectionOrigin = new BlockPos(chunkPos.x << 4, sy << 4, chunkPos.z << 4);
            long key = SectionPos.asLong(chunkPos.x, sy, chunkPos.z);
            try
            {
                SeamlessGhostSectionGpuStore.invalidate(dimension, key);
                int minY = Config.SURFACE_PATCH_SEA_LEVEL.get() + 1;
                forDim.put(key, SeamlessGhostSectionMesher.mesh(ghost, sectionOrigin, minY));
            }
            catch (Exception e)
            {
                Auralithpioneerinitiative.LOGGER.warn("[Auralith] Failed to mesh ghost section {},{},{} for {}: {}", chunkPos.x, sy, chunkPos.z, dimension.location(), e.toString());
            }
        }
    }

    public static void clearDimension(ResourceKey<Level> dimension)
    {
        SECTIONS.remove(dimension);
        SeamlessGhostSectionGpuStore.clearDimension(dimension);
    }

    public static Map<Long, SeamlessGhostSectionMesher.MeshedSection> sectionsFor(ResourceKey<Level> dimension)
    {
        return SECTIONS.get(dimension);
    }

    public static int renderRadiusSections()
    {
        return RENDER_RADIUS_SECTIONS;
    }

    public static void render(RenderTarget target, ClientLevel ghost, ResourceKey<Level> dimension, BlockPos anchor, int pixelWidth, int pixelHeight)
    {
        Map<Long, SeamlessGhostSectionMesher.MeshedSection> forDim = SECTIONS.get(dimension);

        int surfaceY = ghost.getHeight(Heightmap.Types.WORLD_SURFACE, anchor.getX(), anchor.getZ());
        double camX = anchor.getX() + 0.5;
        double camY = surfaceY + 2.0;
        double camZ = anchor.getZ() + 0.5;

        target.bindWrite(true);
        RenderSystem.viewport(0, 0, pixelWidth, pixelHeight);
        RenderSystem.clearColor(0.6f, 0.8f, 1.0f, 1.0f);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        if (forDim == null || forDim.isEmpty())
        {
            target.unbindWrite();
            return;
        }

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70.0), (float) pixelWidth / (float) pixelHeight, 0.05f, 512f);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0f));
        poseStack.translate(-camX, -camY, -camZ);
        Matrix4f viewMatrix = poseStack.last().pose();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.lightTexture().turnOnLightLayer();

        int sax = anchor.getX() >> 4, saz = anchor.getZ() >> 4, say = surfaceY >> 4;
        int drawn = 0;
        for (Map.Entry<Long, SeamlessGhostSectionMesher.MeshedSection> entry : forDim.entrySet())
        {
            long key = entry.getKey();
            int sx = SectionPos.x(key), sy = SectionPos.y(key), sz = SectionPos.z(key);
            if (Math.abs(sx - sax) > RENDER_RADIUS_SECTIONS || Math.abs(sz - saz) > RENDER_RADIUS_SECTIONS || Math.abs(sy - say) > RENDER_RADIUS_SECTIONS) continue;

            VertexBuffer buffer = SeamlessGhostSectionGpuStore.getOrUpload(dimension, key, entry.getValue());
            if (buffer == null) continue;

            buffer.bind();
            buffer.drawWithShader(viewMatrix, projection, RenderSystem.getShader());
            VertexBuffer.unbind();
            drawn++;
        }

        mc.gameRenderer.lightTexture().turnOffLightLayer();
        target.unbindWrite();
    }
}