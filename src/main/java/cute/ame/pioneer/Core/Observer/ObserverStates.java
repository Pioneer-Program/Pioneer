package cute.ame.pioneer.Core.Observer;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class ObserverStates
{
    public static ObserverState beginFrame(Level level, Vec3 rawCamPos, float partialTick)
    {
        return resolve(level, rawCamPos, partialTick);
    }

    public static ObserverState resolve(Level level, Vec3 rawCamPos, float partialTick)
    {
        Optional<PlanetDefinition> body = surfaceHost(level);
        if (body.isEmpty())
            return ObserverState.DEEP_SPACE;

        return ObserverState.fromSurfaceBlocks(body.get(), rawCamPos.x, rawCamPos.y, rawCamPos.z, ObserverState.Origin.SURFACE_BLOCKS);
    }

    public static Optional<PlanetDefinition> surfaceHost(Level level)
    {
        Optional<PioneerAPI.DimensionBinding> bOpt = PioneerAPI.getBindingForDimension(level.dimension());
        if (bOpt.isEmpty())
            return Optional.empty();

        PioneerAPI.DimensionBinding binding = bOpt.get();
        if (binding.type() != PioneerAPI.BindingType.SURFACE || binding.planetId() == null)
            return Optional.empty();

        Optional<SolarSystemDefinition> sOpt = PioneerAPI.getSolarSystem(binding.systemId());
        return sOpt.flatMap(s -> s.findById(binding.planetId()));
    }
}