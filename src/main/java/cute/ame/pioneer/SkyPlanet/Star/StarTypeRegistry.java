package cute.ame.pioneer.SkyPlanet.Star;

import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StarTypeRegistry
{
    private static final Map<ResourceLocation, StarTypeRenderer> TYPES = new ConcurrentHashMap<>();

    public static void register(ResourceLocation id, StarTypeRenderer renderer)
    {
        StarTypeRenderer existing = TYPES.putIfAbsent(id, renderer);
        if (existing != null) Pioneer.LOGGER.warn("[Pioneer] Star type '{}' registered twice: keeping the first registration", id);
    }

    public static StarTypeRenderer get(ResourceLocation id)
    {
        StarTypeRenderer renderer = TYPES.get(id);
        if (renderer == null)
        {
            Pioneer.LOGGER.warn("[Pioneer] No star type registered for '{}', falling back to main_sequence (registered ids: {})", id, TYPES.keySet());
            return TYPES.get(BuiltinStarTypes.MAIN_SEQUENCE);
        }
        return renderer;
    }

    public static boolean isRegistered(ResourceLocation id)
    {
        return TYPES.containsKey(id);
    }
}
