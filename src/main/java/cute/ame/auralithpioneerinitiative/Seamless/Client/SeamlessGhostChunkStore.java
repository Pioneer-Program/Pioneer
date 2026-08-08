package cute.ame.auralithpioneerinitiative.Seamless.Client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SeamlessGhostChunkStore
{
    private record Entry(ChunkPos pos, byte[] chunkPacketBytes) {}

    private static final Map<ResourceKey<Level>, Map<ChunkPos, Entry>> DATA = new ConcurrentHashMap<>();

    public static void store(ResourceKey<Level> target, ChunkPos pos, byte[] chunkPacketBytes)
    {
        DATA.computeIfAbsent(target, k -> new ConcurrentHashMap<>()).put(pos, new Entry(pos, chunkPacketBytes));
    }

    public static boolean has(ResourceKey<Level> target, ChunkPos pos)
    {
        Map<ChunkPos, Entry> forDim = DATA.get(target);
        return forDim != null && forDim.containsKey(pos);
    }

    public static boolean hasAnyDataFor(ResourceKey<Level> target)
    {
        Map<ChunkPos, Entry> forDim = DATA.get(target);
        return forDim != null && !forDim.isEmpty();
    }

    public static byte[] getRawPacketBytes(ResourceKey<Level> target, ChunkPos pos)
    {
        Map<ChunkPos, Entry> forDim = DATA.get(target);
        if (forDim == null) return null;
        Entry e = forDim.get(pos);
        return e == null ? null : e.chunkPacketBytes();
    }

    public static int countCovered(ResourceKey<Level> target, Set<ChunkPos> wanted)
    {
        Map<ChunkPos, Entry> forDim = DATA.get(target);
        if (forDim == null) return 0;
        int count = 0;
        for (ChunkPos pos : wanted)
            if (forDim.containsKey(pos)) count++;

        return count;
    }

    public static void clear(ResourceKey<Level> target)
    {
        DATA.remove(target);
    }

    public static void reset()
    {
        DATA.clear();
    }
}
