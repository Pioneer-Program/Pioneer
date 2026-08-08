package cute.ame.auralithpioneerinitiative.Core.Render.Cache;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public final class MeshManager<K>
{
    private final int maxEntries;
    private final LinkedHashMap<K, VertexBuffer> cache = new LinkedHashMap<>(64, 0.75f, true);

    public MeshManager(int maxEntries)
    {
        this.maxEntries = maxEntries;
    }

    public VertexBuffer getOrBuild(K key, VertexBuffer.Usage usage, Supplier<MeshData> builder)
    {
        RenderSystem.assertOnRenderThreadOrInit();

        VertexBuffer existing = cache.get(key);
        if (existing != null) return existing;

        MeshData mesh = builder.get();
        VertexBuffer buffer = new VertexBuffer(usage);
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();

        cache.put(key, buffer);
        evictIfNeeded();
        return buffer;
    }

    public void invalidate(K key)
    {
        RenderSystem.assertOnRenderThreadOrInit();
        VertexBuffer removed = cache.remove(key);
        if (removed != null) removed.close();
    }

    public void clear()
    {
        RenderSystem.assertOnRenderThreadOrInit();
        for (VertexBuffer buffer : cache.values()) buffer.close();
        cache.clear();
    }

    public int size() { return cache.size(); }

    private void evictIfNeeded()
    {
        if (cache.size() <= maxEntries) return;
        var it = cache.entrySet().iterator();

        while (cache.size() > maxEntries && it.hasNext())
        {
            var entry = it.next();
            entry.getValue().close();
            it.remove();
            Auralithpioneerinitiative.LOGGER.debug("[Auralith] MeshManager evicted '{}' ({} entries)", entry.getKey(), cache.size());
        }
    }
}
