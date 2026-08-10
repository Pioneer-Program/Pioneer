package cute.ame.pioneer.Core.Render.Cache;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import cute.ame.pioneer.Pioneer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class TextureManager<G>
{
    private static final class Entry<G>
    {
        final G group;
        final ResourceLocation[] handles;
        int refCount;

        Entry(G group, ResourceLocation[] handles)
        {
            this.group = group;
            this.handles = handles;
        }
    }

    private final int maxIdleEntries;
    private final Map<String, Entry<G>> cache = new LinkedHashMap<>(64, 0.75f, true);

    public TextureManager(int maxIdleEntries)
    {
        this.maxIdleEntries = maxIdleEntries;
    }

    public G acquire(String cacheKey, String[] namer, Function<Integer, NativeImage> facesGenerator, Function<ResourceLocation[], G> assembler)
    {
        RenderSystem.assertOnRenderThreadOrInit();

        Entry<G> existing = cache.get(cacheKey);
        if (existing != null)
        {
            existing.refCount++;
            return existing.group;
        }

        ResourceLocation[] handles = new ResourceLocation[namer.length];
        for (int i = 0; i < namer.length; i++)
            handles[i] = register(facesGenerator.apply(i), cacheKey, namer[i]);

        G group = assembler.apply(handles);
        Entry<G> entry = new Entry<>(group, handles);
        entry.refCount = 1;
        cache.put(cacheKey, entry);

        evictIfNeeded();
        return group;
    }

    public void release(String cacheKey)
    {
        Entry<G> entry = cache.get(cacheKey);
        if (entry == null) return;
        entry.refCount = Math.max(0, entry.refCount - 1);
    }

    public void clear()
    {
        RenderSystem.assertOnRenderThreadOrInit();
        for (Entry<G> entry : cache.values()) closeEntry(entry);
        cache.clear();
    }

    public int size() { return cache.size(); }

    private void evictIfNeeded()
    {
        if (cache.size() <= maxIdleEntries) return;

        var it = cache.entrySet().iterator();
        while (cache.size() > maxIdleEntries && it.hasNext())
        {
            var mapEntry = it.next();
            Entry<G> entry = mapEntry.getValue();
            if (entry.refCount > 0) continue;

            closeEntry(entry);
            it.remove();
            Pioneer.LOGGER.debug("[Auralith] TextureManager evicted '{}' ({} entries)", mapEntry.getKey(), cache.size());
        }
    }

    private void closeEntry(Entry<G> entry)
    {
        for (ResourceLocation rl : entry.handles)
        {
            var tex = Minecraft.getInstance().getTextureManager().getTexture(rl, null);
            if (tex instanceof DynamicTexture dyn) dyn.close();
            Minecraft.getInstance().getTextureManager().release(rl);
        }
    }

    private static ResourceLocation register(NativeImage image, String groupKey, String faceName)
    {
        DynamicTexture dynTex = new DynamicTexture(image);
        String safeName = "generated/" + groupKey.replace(':', '_').replace('/', '_').replace('@', '_') + "_" + faceName;
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, safeName);
        Minecraft.getInstance().getTextureManager().register(rl, dynTex);
        GlStateManager._bindTexture(dynTex.getId());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GlStateManager._bindTexture(0);
        return rl;
    }
}
