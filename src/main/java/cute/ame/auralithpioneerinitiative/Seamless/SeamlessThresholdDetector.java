package cute.ame.auralithpioneerinitiative.Seamless;

import cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI;
import cute.ame.auralithpioneerinitiative.Config;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.PlanetDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.SolarSystemDefinition;
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
        Optional<AuralithAPI.DimensionBinding> bindingOpt = AuralithAPI.getBindingForDimension(dim);
        if (bindingOpt.isEmpty()) return ThresholdResult.NONE_RESULT;

        AuralithAPI.DimensionBinding binding = bindingOpt.get();

        return switch (binding.type())
        {
            case SURFACE -> evaluateSurface(player);
            default -> ThresholdResult.NONE_RESULT;
        };
    }

    private static ThresholdResult evaluateSurface(ServerPlayer player)
    {
        double entryY = Config.ORBIT_ENTRY_Y.get();
        double margin = surfaceApproachMarginFor(player);

        if (player.getY() < entryY - margin) return ThresholdResult.NONE_RESULT;

        return new ThresholdResult(ThresholdKind.SURFACE_APPROACHING_ORBIT, Optional.empty(), player.getY());
    }

    private static ThresholdResult evaluateSpaceProximity(ServerPlayer player, AuralithAPI.DimensionBinding binding)
    {
        Optional<SolarSystemDefinition> systemOpt = AuralithAPI.getSolarSystem(binding.systemId());
        if (systemOpt.isEmpty()) return ThresholdResult.NONE_RESULT;

        SolarSystemDefinition system = systemOpt.get();
        List<PlanetDefinition> planets = system.allPlanetsFlat();

        PlanetDefinition closest = null;
        double closestDist = Double.MAX_VALUE;

        for (PlanetDefinition planet : planets)
        {
            double[] pos = planet.currentWorldPosition(player.level().getGameTime());
            double dx = player.getX() - pos[0];
            double dy = player.getY() - pos[1];
            double dz = player.getZ() - pos[2];
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double margin = approachMarginFor(player, planet.approachRadius());
            double approachRadius = planet.approachRadius() + margin;
            if (dist <= approachRadius && dist < closestDist)
            {
                closest = planet;
                closestDist = dist;
            }
        }

        if (closest == null) return ThresholdResult.NONE_RESULT;
        return new ThresholdResult(ThresholdKind.SPACE_APPROACHING_PLANET, Optional.of(closest), closestDist);
    }

    private static double surfaceApproachMarginFor(ServerPlayer player)
    {
        double speed = player.getDeltaMovement().length() * 20.0;
        double velocityMargin = speed * 2.0;
        double minMargin = 32.0;
        return Math.max(minMargin, velocityMargin);
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