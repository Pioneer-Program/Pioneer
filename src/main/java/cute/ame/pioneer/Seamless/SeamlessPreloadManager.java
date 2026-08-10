package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SeamlessPreloadManager
{
    private static final TicketType<ChunkPos> SEAMLESS_PRELOAD_TICKET = TicketType.create("auralith_seamless_preload", java.util.Comparator.comparingLong(ChunkPos::toLong), 100);

    private static final int TICKET_LEVEL = 31;
    private record PreloadKey(UUID player, ResourceKey<Level> target) {}

    private static final Map<PreloadKey, Set<ChunkPos>> ACTIVE_PRELOADS = new HashMap<>();

    public static void preload(UUID player, ServerLevel targetLevel, BlockPos anchorPos, int radiusChunks)
    {
        ResourceKey<Level> targetKey = targetLevel.dimension();
        PreloadKey key = new PreloadKey(player, targetKey);

        ChunkPos center = new ChunkPos(anchorPos);
        Set<ChunkPos> newSet = new HashSet<>();
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++)
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++)
                newSet.add(new ChunkPos(center.x + dx, center.z + dz));

        Set<ChunkPos> previous = ACTIVE_PRELOADS.getOrDefault(key, Set.of());

        for (ChunkPos pos : newSet)
            if (!previous.contains(pos))
                targetLevel.getChunkSource().addRegionTicket(SEAMLESS_PRELOAD_TICKET, pos, TICKET_LEVEL, pos);

        for (ChunkPos pos : previous)
            if (!newSet.contains(pos))
                targetLevel.getChunkSource().removeRegionTicket(SEAMLESS_PRELOAD_TICKET, pos, TICKET_LEVEL, pos);

        ACTIVE_PRELOADS.put(key, newSet);
    }

    public static boolean isFullyLoaded(UUID player, ResourceKey<Level> targetKey, ServerLevel targetLevel)
    {
        PreloadKey key = new PreloadKey(player, targetKey);
        Set<ChunkPos> forced = ACTIVE_PRELOADS.get(key);
        if (forced == null || forced.isEmpty()) return false;
        for (ChunkPos pos : forced) if (!targetLevel.hasChunk(pos.x, pos.z)) return false;

        return true;
    }

    public static Set<ChunkPos> getForcedSnapshot(UUID player, ResourceKey<Level> targetKey)
    {
        PreloadKey key = new PreloadKey(player, targetKey);
        Set<ChunkPos> forced = ACTIVE_PRELOADS.get(key);
        return forced == null ? Set.of() : Set.copyOf(forced);
    }

    public static void release(UUID player, ResourceKey<Level> targetKey, ServerLevel targetLevel)
    {
        PreloadKey key = new PreloadKey(player, targetKey);
        Set<ChunkPos> previous = ACTIVE_PRELOADS.remove(key);
        if (previous == null) return;

        for (ChunkPos pos : previous) targetLevel.getChunkSource().removeRegionTicket(SEAMLESS_PRELOAD_TICKET, pos, TICKET_LEVEL, pos);
        Pioneer.LOGGER.debug("[Auralith] Released {} preloaded chunks for player {} in {}", previous.size(), player, targetKey.location());
    }

    public static void releaseAllForPlayer(UUID player, java.util.function.Function<ResourceKey<Level>, ServerLevel> levelLookup)
    {
        ACTIVE_PRELOADS.keySet().removeIf(key ->
        {
            if (!key.player().equals(player)) return false;
            ServerLevel level = levelLookup.apply(key.target());
            if (level != null)
                for (ChunkPos pos : ACTIVE_PRELOADS.get(key))
                    level.getChunkSource().removeRegionTicket(SEAMLESS_PRELOAD_TICKET, pos, TICKET_LEVEL, pos);

            return true;
        });
    }
}