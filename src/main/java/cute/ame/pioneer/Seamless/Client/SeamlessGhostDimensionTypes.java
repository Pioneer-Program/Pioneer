package cute.ame.pioneer.Seamless.Client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SeamlessGhostDimensionTypes
{
    private static final Map<ResourceKey<Level>, ResourceKey<DimensionType>> KNOWN = new ConcurrentHashMap<>();

    public static void remember(ResourceKey<Level> target, ResourceKey<DimensionType> dimensionType)
    {
        KNOWN.put(target, dimensionType);
    }

    public static ResourceKey<DimensionType> get(ResourceKey<Level> target)
    {
        return KNOWN.get(target);
    }

    public static void forget(ResourceKey<Level> target)
    {
        KNOWN.remove(target);
    }

    public static void reset()
    {
        KNOWN.clear();
    }
}
