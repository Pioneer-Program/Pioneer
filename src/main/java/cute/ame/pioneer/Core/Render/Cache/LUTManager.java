package cute.ame.pioneer.Core.Render.Cache;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import cute.ame.pioneer.Pioneer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class LUTManager
{
    private record Key(String id, int paramHash) {}

    private static final int MAX_ENTRIES = 128;
    private static final Map<Key, ResourceLocation> CACHE = new LinkedHashMap<>(32, 0.75f, true);

    public static ResourceLocation getOrGenerate(String id, int paramHash, Supplier<NativeImage> generator)
    {
        return getOrGenerate(id, paramHash, generator, true);
    }

    public static ResourceLocation getOrGenerate(String id, int paramHash, Supplier<NativeImage> generator, boolean linear)
    {
        RenderSystem.assertOnRenderThreadOrInit();

        Key key = new Key(id, paramHash);
        ResourceLocation existing = CACHE.get(key);
        if (existing != null) return existing;

        NativeImage image = generator.get();
        ResourceLocation rl = register(image, id, paramHash, linear);
        CACHE.put(key, rl);
        evictIfNeeded();
        return rl;
    }

    public static void invalidate(String id, int paramHash)
    {
        RenderSystem.assertOnRenderThreadOrInit();
        ResourceLocation removed = CACHE.remove(new Key(id, paramHash));
        if (removed != null) closeTexture(removed);
    }

    public static void clear()
    {
        RenderSystem.assertOnRenderThreadOrInit();
        for (ResourceLocation rl : CACHE.values()) closeTexture(rl);
        CACHE.clear();
    }

    public static int size() { return CACHE.size(); }

    private static void evictIfNeeded()
    {
        if (CACHE.size() <= MAX_ENTRIES) return;
        var it = CACHE.entrySet().iterator();
        if (it.hasNext())
        {
            var e = it.next();
            closeTexture(e.getValue());
            it.remove();
            Pioneer.LOGGER.debug("[Auralith] LUTManager evicted '{}' ({} entries)", e.getKey(), CACHE.size());
        }
    }

    private static void closeTexture(ResourceLocation rl)
    {
        var tex = Minecraft.getInstance().getTextureManager().getTexture(rl, null);
        if (tex instanceof DynamicTexture dyn) dyn.close();
        Minecraft.getInstance().getTextureManager().release(rl);
    }

    private static ResourceLocation register(NativeImage image, String id, int paramHash, boolean linear)
    {
        DynamicTexture dynTex = new DynamicTexture(image);
        String safeName = "generated/lut/" + id.replace(':', '_').replace('/', '_') + "_" + Integer.toHexString(paramHash);
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, safeName);
        Minecraft.getInstance().getTextureManager().register(rl, dynTex);

        int filter = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GlStateManager._bindTexture(dynTex.getId());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._bindTexture(0);
        return rl;
    }
}
