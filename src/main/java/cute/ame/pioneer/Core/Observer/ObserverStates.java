package cute.ame.pioneer.Core.Observer;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ObserverStates
{
    @FunctionalInterface
    public interface Source
    {
        @Nullable ObserverState resolve(Level level, Vec3 rawCamPos, float partialTick);

        default int priority() { return 0; }
    }

    private static final List<Source> SOURCES = new ArrayList<>(4);

    private static ObserverState current = ObserverState.DEEP_SPACE;
    private static long frameStamp = Long.MIN_VALUE;

    public static void register(Source source)
    {
        SOURCES.add(source);
        SOURCES.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    }

    public static ObserverState beginFrame(Level level, Vec3 rawCamPos, float partialTick, long stamp)
    {
        if (stamp == frameStamp) return current;
        frameStamp = stamp;
        current = resolve(level, rawCamPos, partialTick);
        return current;
    }

    public static ObserverState beginFrame(Level level, Vec3 rawCamPos, float partialTick)
    {
        current = resolve(level, rawCamPos, partialTick);
        return current;
    }

    public static ObserverState current() { return current; }

    public static ObserverState resolve(Level level, Vec3 rawCamPos, float partialTick)
    {
        for (int i = 0, n = SOURCES.size(); i < n; i++)
        {
            ObserverState s = SOURCES.get(i).resolve(level, rawCamPos, partialTick);
            if (s != null) return s;
        }
        return fromBlocks(level, rawCamPos);
    }

    public static ObserverState fromBlocks(Level level, Vec3 camPos)
    {
        PlanetDefinition body = surfaceBodyOf(level);
        if (body == null) return ObserverState.DEEP_SPACE;
        return ObserverState.fromSurfaceBlocks(body, camPos.x, camPos.y, camPos.z, ObserverState.Origin.SURFACE_BLOCKS);
    }

    public static @Nullable PlanetDefinition surfaceBodyOf(Level level)
    {
        Optional<PioneerAPI.DimensionBinding> bOpt = PioneerAPI.getBindingForDimension(level.dimension());
        if (bOpt.isEmpty()) return null;

        PioneerAPI.DimensionBinding binding = bOpt.get();
        if (binding.type() != PioneerAPI.BindingType.SURFACE || binding.planetId() == null) return null;

        Optional<SolarSystemDefinition> sOpt = PioneerAPI.getSolarSystem(binding.systemId());
        return sOpt.flatMap(s -> s.findById(binding.planetId())).orElse(null);
    }

    public static void invalidate()
    {
        frameStamp = Long.MIN_VALUE;
        current = ObserverState.DEEP_SPACE;
    }
}