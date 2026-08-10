package cute.ame.pioneer.Seamless;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SeamlessTransitionState
{
    private static final Set<ResourceKey<Level>> ACTIVE_DIMENSIONS = ConcurrentHashMap.newKeySet();
    private static volatile boolean transitioning = false;

    public static boolean isTransitioning()
    {
        return transitioning;
    }

    public static boolean involves(ResourceKey<Level> dim)
    {
        return ACTIVE_DIMENSIONS.contains(dim);
    }

    public static void beginTransition(ResourceKey<Level> from, ResourceKey<Level> to)
    {
        ACTIVE_DIMENSIONS.add(from);
        ACTIVE_DIMENSIONS.add(to);
        transitioning = true;
    }

    public static void endTransition(ResourceKey<Level> from, ResourceKey<Level> to)
    {
        ACTIVE_DIMENSIONS.remove(from);
        ACTIVE_DIMENSIONS.remove(to);
        transitioning = !ACTIVE_DIMENSIONS.isEmpty();
    }

    public static void forgetInvolving(ResourceKey<Level> dim)
    {
        ACTIVE_DIMENSIONS.remove(dim);
        transitioning = !ACTIVE_DIMENSIONS.isEmpty();
    }

    public static void reset()
    {
        ACTIVE_DIMENSIONS.clear();
        transitioning = false;
    }
}