package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Observer.ObserverState;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class SeamlessThresholdDetector
{
    private static final int CHECK_INTERVAL_TICKS = 5;

    public enum ThresholdKind { NONE, SURFACE_APPROACHING_ORBIT, SPACE_APPROACHING_PLANET }

    public record ThresholdResult(ThresholdKind kind, Optional<PlanetDefinition> targetPlanet, double distanceOrHeight)
    {
        public static final ThresholdResult NONE_RESULT = new ThresholdResult(ThresholdKind.NONE, Optional.empty(), 0.0);
    }

    public static boolean shouldCheck(ServerPlayer player)
    {
        return player.tickCount % CHECK_INTERVAL_TICKS == 0;
    }

    public static ThresholdResult evaluate(ServerPlayer player)
    {
        ResourceKey<Level> dim = player.level().dimension();
        Optional<PioneerAPI.DimensionBinding> bindingOpt = PioneerAPI.getBindingForDimension(dim);
        if (bindingOpt.isEmpty()) return ThresholdResult.NONE_RESULT;

        PioneerAPI.DimensionBinding binding = bindingOpt.get();

        return switch (binding.type())
        {
            case SURFACE -> evaluateSurface(player);
            case SPACE -> evaluateSpaceProximity(player, binding);
        };
    }

    private static ThresholdResult evaluateSurface(ServerPlayer player)
    {
        ObserverState obs = ObserverStates.resolve(player.level(), player.position(), 0f);
        if (!obs.hasBody()) return ThresholdResult.NONE_RESULT;

        double altKm = obs.altitudeKm();
        double entryKm = Config.ORBIT_ENTRY_ALTITUDE_KM.get();
        if (altKm < entryKm - surfaceApproachMarginKm(player, obs)) return ThresholdResult.NONE_RESULT;

        return new ThresholdResult(ThresholdKind.SURFACE_APPROACHING_ORBIT, Optional.empty(), altKm);
    }

    private static ThresholdResult evaluateSpaceProximity(ServerPlayer player, PioneerAPI.DimensionBinding binding)
    {
        Optional<SolarSystemDefinition> systemOpt = PioneerAPI.getSolarSystem(binding.systemId());
        if (systemOpt.isEmpty()) return ThresholdResult.NONE_RESULT;

        List<PlanetDefinition> planets = systemOpt.get().allPlanetsFlat();

        PlanetDefinition closest = null;
        double closestDist = Double.MAX_VALUE;

        for (PlanetDefinition planet : planets)
        {
            if (planet.dimension().isEmpty()) continue;

            double[] pos = planet.currentWorldPosition(player.level().getGameTime());
            double dx = player.getX() - pos[0];
            double dy = player.getY() - pos[1];
            double dz = player.getZ() - pos[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double approachRadius = planet.approachRadius() + approachMarginFor(player, planet.approachRadius());
            if (dist <= approachRadius && dist < closestDist)
            {
                closest = planet;
                closestDist = dist;
            }
        }

        if (closest == null) return ThresholdResult.NONE_RESULT;
        return new ThresholdResult(ThresholdKind.SPACE_APPROACHING_PLANET, Optional.of(closest), closestDist);
    }

    private static double surfaceApproachMarginKm(ServerPlayer player, ObserverState obs)
    {
        double blocksPerTick = player.getDeltaMovement().length();
        double kmPerTick = blocksPerTick * obs.body().verticalScale() * 1.0e-3;
        double travelledBetweenChecks = kmPerTick * CHECK_INTERVAL_TICKS * 2.0;
        return Math.max(0.05, travelledBetweenChecks);
    }

    private static double approachMarginFor(ServerPlayer player, double approachRadius)
    {
        double speed = player.getDeltaMovement().length() * 20.0;
        double velocityMargin = speed * 2.0;
        double maxMargin = approachRadius * 0.5;
        double minMargin = Math.min(2.0, approachRadius * 0.1);
        return Math.min(maxMargin, Math.max(minMargin, velocityMargin));
    }
}