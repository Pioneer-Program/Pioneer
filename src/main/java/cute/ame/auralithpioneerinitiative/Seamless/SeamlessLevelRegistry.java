package cute.ame.auralithpioneerinitiative.Seamless;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostChunkStore;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostInjectQueue;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostSectionRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SeamlessLevelRegistry
{
    public static final int MAX_RETAINED = 5;

    private static final class Entry
    {
        ClientLevel level;
        int refCount;
        long lastTouched;
    }

    private static final Map<ResourceKey<Level>, Entry> RETAINED = new LinkedHashMap<>();

    public static synchronized boolean hasRoomFor(ResourceKey<Level> target)
    {
        if (RETAINED.containsKey(target)) return true;
        if (RETAINED.size() < MAX_RETAINED) return true;
        return findEvictionCandidate() != null;
    }

    public static synchronized void retain(ResourceKey<Level> key, ClientLevel level)
    {
        Entry entry = RETAINED.get(key);
        if (entry == null)
        {
            if (RETAINED.size() >= MAX_RETAINED)
            {
                ResourceKey<Level> evictKey = findEvictionCandidate();
                if (evictKey != null) forceRelease(evictKey);
                else Auralithpioneerinitiative.LOGGER.warn("[Auralith] SeamlessLevelRegistry at cap ({}) with no evictable entry, retaining {} anyway, exceeding cap", MAX_RETAINED, key.location());
            }
            entry = new Entry();
            entry.level = level;
            RETAINED.put(key, entry);
        }
        entry.refCount++;
        entry.lastTouched = System.nanoTime();
    }

    public static synchronized void release(ResourceKey<Level> key)
    {
        Entry entry = RETAINED.get(key);
        if (entry == null) return;
        entry.refCount = Math.max(0, entry.refCount - 1);
        entry.lastTouched = System.nanoTime();
        if (entry.refCount == 0)
        {
            RETAINED.remove(key);
            Auralithpioneerinitiative.LOGGER.debug("[Auralith] Released ClientLevel {} (refCount=0)", key.location());
            dropAuxiliaryData(key);
        }
    }

    private static void forceRelease(ResourceKey<Level> key)
    {
        RETAINED.remove(key);
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Force-evicted ClientLevel {} to respect cap of {}", key.location(), MAX_RETAINED);
        dropAuxiliaryData(key);
    }

    private static void dropAuxiliaryData(ResourceKey<Level> key)
    {
        SeamlessGhostChunkStore.clear(key);
        SeamlessGhostInjectQueue.clear(key);
        SeamlessGhostSectionRenderer.clearDimension(key);
    }

    private static ResourceKey<Level> findEvictionCandidate()
    {
        ResourceKey<Level> oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<ResourceKey<Level>, Entry> e : RETAINED.entrySet())
        {
            if (e.getValue().refCount > 0) continue;
            if (e.getValue().lastTouched < oldestTime)
            {
                oldestTime = e.getValue().lastTouched;
                oldest = e.getKey();
            }
        }
        return oldest;
    }

    public static synchronized Optional<ClientLevel> get(ResourceKey<Level> key)
    {
        Entry entry = RETAINED.get(key);
        return entry == null ? Optional.empty() : Optional.of(entry.level);
    }

    public static synchronized boolean isRetained(ResourceKey<Level> key)
    {
        return RETAINED.containsKey(key);
    }

    public static synchronized int retainedCount()
    {
        return RETAINED.size();
    }

    public static synchronized void reset()
    {
        RETAINED.clear();
    }
}