package cute.ame.pioneer.Seamless.Client;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Seamless.SeamlessLevelRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public final class SeamlessGhostLevelBuilder
{
    private static final int GHOST_VIEW_RADIUS_CHUNKS = 40;

    public static void ensureAndInject(ResourceKey<Level> target, ChunkPos pos)
    {
        SeamlessGhostInjectQueue.enqueue(target, pos);
    }

    public static synchronized void injectNow(ResourceKey<Level> target, ChunkPos pos)
    {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;

        ClientLevel ghost = SeamlessLevelRegistry.get(target).orElse(null);
        if (ghost == null)
        {
            ghost = tryCreateGhostLevel(target, connection);
            if (ghost == null) return;
            SeamlessLevelRegistry.retain(target, ghost);
        }
        if (!SeamlessGhostSectionRenderer.isWithinRenderMargin(target, pos)) return;

        byte[] bytes = SeamlessGhostChunkStore.getRawPacketBytes(target, pos);
        if (bytes == null) return;

        injectChunk(ghost, target, pos, bytes);
    }

    public static void resyncGameTime(ClientLevel ghost)
    {
        ClientLevel real = Minecraft.getInstance().level;
        if (real == null || real == ghost) return;

        ghost.setGameTime(real.getGameTime());
        ghost.setDayTime(real.getDayTime());
    }

    private static ClientLevel tryCreateGhostLevel(ResourceKey<Level> target, ClientPacketListener connection)
    {
        ResourceKey<DimensionType> dimTypeKey = SeamlessGhostDimensionTypes.get(target);
        if (dimTypeKey == null)
        {
            Pioneer.LOGGER.warn("[Auralith] No known dimension_type for {}, can't build ghost level yet (preload hint missing/late?)", target.location());
            return null;
        }

        try
        {
            Holder<DimensionType> dimTypeHolder = connection.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(dimTypeKey);

            ClientLevel.ClientLevelData levelData = new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false);
            int viewDistance = GHOST_VIEW_RADIUS_CHUNKS;

            LevelRenderer dedicatedRenderer = SeamlessGhostRenderer.sharedLevelRenderer();
            ClientLevel ghost = new SeamlessGhostClientLevel(connection, levelData, target, dimTypeHolder, viewDistance, viewDistance, Minecraft.getInstance()::getProfiler, dedicatedRenderer, false, 0L);
            BlockPos anchor = SeamlessGhostAnchors.get(target);
            if (anchor != null)
            {
                ghost.getChunkSource().updateViewCenter(anchor.getX() >> 4, anchor.getZ() >> 4);
                ghost.getChunkSource().updateViewRadius(viewDistance);
            }

            resyncGameTime(ghost);
            Pioneer.LOGGER.debug("[Auralith] Built static ghost ClientLevel for {} (dimension_type={})", target.location(), dimTypeKey.location());
            return ghost;
        }
        catch (Exception e)
        {
            Pioneer.LOGGER.warn("[Auralith] Failed to construct ghost ClientLevel for {}: {}", target.location(), e.toString());
            return null;
        }
    }

    private static void scheduleNeighborRemeshes(ClientLevel ghost, ResourceKey<Level> target, ChunkPos pos)
    {
        int[][] cardinalOffsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] off : cardinalOffsets)
        {
            ChunkPos neighbor = new ChunkPos(pos.x + off[0], pos.z + off[1]);
            if (!SeamlessGhostSectionRenderer.isWithinRenderMargin(target, neighbor)) continue;
            if (!ghost.hasChunk(neighbor.x, neighbor.z)) continue;
            SeamlessGhostInjectQueue.enqueueRemesh(target, neighbor);
        }
    }

    private static void injectChunk(ClientLevel ghost, ResourceKey<Level> target, ChunkPos pos, byte[] bytes)
    {
        try
        {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), ghost.registryAccess());
            ClientboundLevelChunkWithLightPacket packet;
            try
            {
                packet = ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buf);
                var chunkData = packet.getChunkData();
                ghost.getChunkSource().replaceWithPacketData(packet.getX(), packet.getZ(), chunkData.getReadBuffer(), chunkData.getHeightmaps(), chunkData.getBlockEntitiesTagsConsumer(packet.getX(), packet.getZ()));
            }
            finally
            {
                buf.release();
            }

            applyLightData(ghost, pos, packet.getLightData());
            SeamlessGhostSectionRenderer.meshChunk(ghost, target, pos);
            scheduleNeighborRemeshes(ghost, target, pos);

            Pioneer.LOGGER.debug("[Auralith] Injected ghost chunk {} into static level {}", pos, target.location());
            if (!ghost.hasChunk(pos.x, pos.z)) Pioneer.LOGGER.warn("[Auralith] Ghost chunk {} for {} reported injected but is NOT present in the level afterward (likely dropped by ClientChunkCache view range)", pos, target.location());
        }
        catch (Exception e)
        {
            Pioneer.LOGGER.warn("[Auralith] Failed to inject ghost chunk {} for {}: {}", pos, target.location(), e.toString());
        }
    }

    private static void applyLightData(ClientLevel ghost, ChunkPos pos, ClientboundLightUpdatePacketData lightData)
    {
        LevelLightEngine lightEngine = ghost.getChunkSource().getLightEngine();
        lightEngine.setLightEnabled(pos, true);

        updateLightLayer(lightEngine, pos, LightLayer.SKY, lightData.getSkyYMask(), lightData.getEmptySkyYMask(), lightData.getSkyUpdates());
        updateLightLayer(lightEngine, pos, LightLayer.BLOCK, lightData.getBlockYMask(), lightData.getEmptyBlockYMask(), lightData.getBlockUpdates());
        lightEngine.setLightEnabled(pos, true);
        lightEngine.runLightUpdates();
    }

    private static void updateLightLayer(LevelLightEngine lightEngine, ChunkPos pos, LightLayer layer, BitSet dataMask, BitSet emptyMask, List<byte[]> updates)
    {
        Iterator<byte[]> iterator = updates.iterator();
        for (int y = lightEngine.getMinLightSection(); y < lightEngine.getMaxLightSection(); ++y)
        {
            int bit = y - lightEngine.getMinLightSection();
            boolean hasData = dataMask.get(bit);
            boolean isEmpty = emptyMask.get(bit);
            if (hasData || isEmpty)
            {
                lightEngine.queueSectionData(layer, SectionPos.of(pos.x, y, pos.z), hasData ? new DataLayer(iterator.next()) : new DataLayer());
                lightEngine.retainData(pos, true);
            }
        }
    }
}