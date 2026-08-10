package cute.ame.pioneer.Seamless.Client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SeamlessGhostAnchors
{
    private static final Map<ResourceKey<Level>, BlockPos> KNOWN = new ConcurrentHashMap<>();

    public static void remember(ResourceKey<Level> target, BlockPos anchor)
    {
        KNOWN.put(target, anchor);
    }

    public static BlockPos get(ResourceKey<Level> target)
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
