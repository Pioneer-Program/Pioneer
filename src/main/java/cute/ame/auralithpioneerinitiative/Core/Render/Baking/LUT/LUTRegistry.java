package cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Core.Render.Cache.LUTManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LUTRegistry
{
    private static final Map<ResourceLocation, LUTBaker> BAKERS = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, LUTBaker baker)
    {
        LUTBaker existing = BAKERS.putIfAbsent(id, baker);
        if (existing != null) Auralithpioneerinitiative.LOGGER.warn("[Auralith] LUT baker '{}' registered twice — keeping the first registration", id);
    }

    public static boolean isRegistered(ResourceLocation id) { return BAKERS.containsKey(id); }

    public static ResourceLocation get(ResourceLocation id, LUTParams params)
    {
        LUTBaker baker = BAKERS.get(id);
        if (baker == null) throw new IllegalStateException("No LUT baker registered for id: " + id + " (registered: " + BAKERS.keySet() + ")");

        return LUTManager.getOrGenerate(id.toString(), params.cacheHash(), () -> baker.bake(params), baker.linearFilter());
    }

    public static void invalidate(ResourceLocation id, LUTParams params)
    {
        LUTManager.invalidate(id.toString(), params.cacheHash());
    }
}
