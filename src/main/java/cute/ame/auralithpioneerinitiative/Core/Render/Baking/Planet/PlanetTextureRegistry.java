package cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlanetTextureRegistry
{
    private static final Map<ResourceLocation, PlanetTextureBaker> GENERATORS = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, PlanetTextureBaker generator)
    {
        PlanetTextureBaker existing = GENERATORS.putIfAbsent(id, generator);
        if (existing != null) Auralithpioneerinitiative.LOGGER.warn("[Auralith] Texture generator '{}' registered twice — keeping the first registration", id);
    }

    public static PlanetTextureBaker get(ResourceLocation id)
    {
        PlanetTextureBaker generator = GENERATORS.get(id);
        if (generator == null) throw new IllegalStateException("No planet texture generator registered for id: " + id + " (registered ids: " + GENERATORS.keySet() + ")");

        return generator;
    }

    public static boolean isRegistered(ResourceLocation id) { return GENERATORS.containsKey(id); }
}
