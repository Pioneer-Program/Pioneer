package cute.ame.pioneer.Seamless.Client;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Seamless.SeamlessLevelRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class SeamlessGhostInjectQueue
{
    private enum Kind { FULL, REMESH_ONLY }
    private record Pending(ResourceKey<Level> target, ChunkPos pos, Kind kind) {}
    private static final ArrayDeque<Pending> QUEUE = new ArrayDeque<>();
    private static final Set<Pending> QUEUED = new HashSet<>();
    private static final long BUDGET_NANOS = 3_000_000L;

    public static void enqueue(ResourceKey<Level> target, ChunkPos pos)
    {
        Pending p = new Pending(target, pos, Kind.FULL);
        if (QUEUED.add(p)) QUEUE.addLast(p);
    }

    public static void enqueueRemesh(ResourceKey<Level> target, ChunkPos pos)
    {
        Pending p = new Pending(target, pos, Kind.REMESH_ONLY);
        if (QUEUED.add(p)) QUEUE.addLast(p);
    }

    public static void drain()
    {
        if (QUEUE.isEmpty()) return;

        long deadline = System.nanoTime() + BUDGET_NANOS;
        int processed = 0;
        while (!QUEUE.isEmpty() && System.nanoTime() < deadline)
        {
            Pending p = QUEUE.pollFirst();
            QUEUED.remove(p);
            try
            {
                if (p.kind() == Kind.FULL) SeamlessGhostLevelBuilder.injectNow(p.target(), p.pos());
                else SeamlessLevelRegistry.get(p.target()).ifPresent(ghost -> SeamlessGhostSectionRenderer.meshChunk(ghost, p.target(), p.pos()));
            }
            catch (Exception e)
            {
                Pioneer.LOGGER.warn("[Auralith] Queued ghost chunk {} failed for {} in {}: {}", p.kind(), p.pos(), p.target().location(), e.toString());
            }
            processed++;
        }

        if (processed > 0) Pioneer.LOGGER.debug("[Auralith] Drained {} queued ghost chunk task(s), {} remaining", processed, QUEUE.size());
    }

    public static void clear(ResourceKey<Level> target)
    {
        QUEUE.removeIf(p -> p.target().equals(target));
        QUEUED.removeIf(p -> p.target().equals(target));
    }

    public static void reset()
    {
        QUEUE.clear();
        QUEUED.clear();
    }
}
