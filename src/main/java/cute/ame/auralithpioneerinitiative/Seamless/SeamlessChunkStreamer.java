package cute.ame.auralithpioneerinitiative.Seamless;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Seamless.Network.ChunkStreamPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SeamlessChunkStreamer
{
    private record StreamKey(UUID player, ResourceKey<Level> target) {}

    private static final Map<StreamKey, Set<ChunkPos>> SENT = new HashMap<>();

    public static int streamReadyChunks(ServerPlayer player, ServerLevel targetLevel, Set<ChunkPos> forcedChunks)
    {
        StreamKey key = new StreamKey(player.getUUID(), targetLevel.dimension());
        Set<ChunkPos> sent = SENT.computeIfAbsent(key, k -> new HashSet<>());

        int sentThisCall = 0;
        for (ChunkPos pos : forcedChunks)
        {
            if (sent.contains(pos)) continue;
            if (!targetLevel.hasChunk(pos.x, pos.z)) continue;

            try
            {
                LevelChunk chunk = targetLevel.getChunk(pos.x, pos.z);

                ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, targetLevel.getLightEngine(), null, null);
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
                try
                {
                    ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(buf, packet);
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);

                    PacketDistributor.sendToPlayer(player, new ChunkStreamPayload(targetLevel.dimension(), pos, bytes));
                    sent.add(pos);
                    sentThisCall++;
                }
                finally
                {
                    buf.release();
                }
            }
            catch (Exception e)
            {
                Auralithpioneerinitiative.LOGGER.warn("[Auralith] Failed to stream chunk {} in {} to {}: {}", pos, targetLevel.dimension().location(), player.getScoreboardName(), e.toString());
            }
        }

        if (sentThisCall == 0 && !forcedChunks.isEmpty())
        {
            long readyNow = forcedChunks.stream().filter(p -> targetLevel.hasChunk(p.x, p.z)).count();
            Auralithpioneerinitiative.LOGGER.debug("[Auralith] streamReadyChunks for {} sent 0 new chunks this call ({} of {} forced chunks are actually loaded, {} already sent previously)", targetLevel.dimension().location(), readyNow, forcedChunks.size(), sent.size());
        }

        return sentThisCall;
    }

    public static void release(UUID player, ResourceKey<Level> target)
    {
        SENT.remove(new StreamKey(player, target));
    }

    public static void releaseAllForPlayer(UUID player)
    {
        SENT.keySet().removeIf(k -> k.player().equals(player));
    }
}
