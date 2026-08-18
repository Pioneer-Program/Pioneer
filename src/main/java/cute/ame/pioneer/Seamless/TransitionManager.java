package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Observer.ObserverState;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.Core.Observer.PlanetCube;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TransitionManager
{
    public enum State { IDLE, SWAPPING, SETTLING }

    private static final class PlayerTransition
    {
        SeamlessThresholdDetector.ThresholdKind kind;
        State state = State.IDLE;
        ResourceKey<Level> fromDim;
        ResourceKey<Level> targetDim;
        BlockPos anchor;
        int settleTicksRemaining;
    }

    private static final int SETTLE_TICKS = 10;
    private static final int LANDING_PROBE_Y = 320;

    private static final Map<UUID, PlayerTransition> PLAYER_STATES = new HashMap<>();

    public static void tick(ServerPlayer player)
    {
        PlayerTransition pt = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new PlayerTransition());

        switch (pt.state)
        {
            case IDLE -> tickIdle(player, pt);
            case SWAPPING -> { }
            case SETTLING -> tickSettling(pt);
        }
    }

    private static void tickIdle(ServerPlayer player, PlayerTransition pt)
    {
        if (!SeamlessThresholdDetector.shouldCheck(player)) return;

        SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
        if (result.kind() == SeamlessThresholdDetector.ThresholdKind.NONE) return;
        if (!crossedHardThreshold(player, result)) return;

        Optional<ResolvedTarget> targetOpt = resolveTarget(player, result);
        if (targetOpt.isEmpty()) return;

        pt.kind = result.kind();
        pt.fromDim = player.level().dimension();
        pt.targetDim = targetOpt.get().dimension();
        pt.anchor = targetOpt.get().anchor();

        doSwap(player, pt);
    }

    private static void doSwap(ServerPlayer player, PlayerTransition pt)
    {
        pt.state = State.SWAPPING;

        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server != null ? server.getLevel(pt.targetDim) : null;
        if (targetLevel == null) { resetToIdle(pt); return; }

        BlockPos landing = pt.anchor;

        if (pt.kind == SeamlessThresholdDetector.ThresholdKind.SPACE_APPROACHING_PLANET)
        {
            targetLevel.getChunk(landing.getX() >> 4, landing.getZ() >> 4);
            int surfaceY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, landing.getX(), landing.getZ());
            landing = new BlockPos(landing.getX(), surfaceY + 1, landing.getZ());
        }

        player.teleportTo(targetLevel, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
        Pioneer.LOGGER.debug("[Pioneer] {} SWAP {} -> {} @ {}", player.getScoreboardName(), pt.fromDim.location(), pt.targetDim.location(), landing.toShortString());

        pt.state = State.SETTLING;
        pt.settleTicksRemaining = SETTLE_TICKS;
    }

    private static void tickSettling(PlayerTransition pt)
    {
        if (--pt.settleTicksRemaining <= 0) resetToIdle(pt);
    }

    private static void resetToIdle(PlayerTransition pt)
    {
        pt.kind = null;
        pt.state = State.IDLE;
        pt.fromDim = null;
        pt.targetDim = null;
        pt.anchor = null;
        pt.settleTicksRemaining = 0;
    }

    public static void onPlayerDisconnect(ServerPlayer player)
    {
        PLAYER_STATES.remove(player.getUUID());
    }

    public static boolean isManaging(ServerPlayer player)
    {
        PlayerTransition pt = PLAYER_STATES.get(player.getUUID());
        return pt != null && pt.state != State.IDLE;
    }

    private record ResolvedTarget(ResourceKey<Level> dimension, BlockPos anchor) {}

    private static Optional<ResolvedTarget> resolveTarget(ServerPlayer player, SeamlessThresholdDetector.ThresholdResult result)
    {
        return switch (result.kind())
        {
            case SURFACE_APPROACHING_ORBIT -> orbitTarget(player);
            case SPACE_APPROACHING_PLANET -> result.targetPlanet().flatMap(planet -> planet.dimension().map(loc -> ResourceKey.create(Registries.DIMENSION, loc)).map(dim -> new ResolvedTarget(dim, surfaceLandingColumn(player, planet))));

            default -> Optional.empty();
        };
    }

    private static Optional<ResolvedTarget> orbitTarget(ServerPlayer player)
    {
        var bindingOpt = PioneerAPI.getBindingForDimension(player.level().dimension());
        if (bindingOpt.isEmpty()) return Optional.empty();

        var systemOpt = PioneerAPI.getSolarSystem(bindingOpt.get().systemId());
        if (systemOpt.isEmpty()) return Optional.empty();

        var planetOpt = systemOpt.get().findById(bindingOpt.get().planetId());
        var spaceDimOpt = systemOpt.get().spaceDimension();
        if (planetOpt.isEmpty() || spaceDimOpt.isEmpty()) return Optional.empty();

        PlanetDefinition planet = planetOpt.get();
        double[] pos = planet.currentWorldPosition(player.level().getGameTime());
        double clearance = planet.approachRadius() + Config.ORBIT_ENTRY_ALTITUDE_KM.get();

        BlockPos anchor = new BlockPos((int) pos[0], (int) (pos[1] + clearance), (int) pos[2]);
        return Optional.of(new ResolvedTarget(ResourceKey.create(Registries.DIMENSION, spaceDimOpt.get()), anchor));
    }

    public static BlockPos surfaceLandingColumn(ServerPlayer player, PlanetDefinition planet)
    {
        double[] center = planet.currentWorldPosition(player.level().getGameTime());
        Vec3 rel = player.position().subtract(center[0], center[1], center[2]);

        ObserverState obs = ObserverState.fromBodyKm(planet, rel.x, rel.y, rel.z, ObserverState.Origin.ORBITAL_STATE);

        int x = (int) Math.round(obs.blockX());
        int z = (int) Math.round(obs.blockZ());
        return new BlockPos(x, LANDING_PROBE_Y, z);
    }

    public static BlockPos homeColumn(PlanetDefinition planet)
    {
        return new BlockPos((int) Math.round(PlanetCube.homeBlockX(planet)), LANDING_PROBE_Y, (int) Math.round(PlanetCube.homeBlockZ(planet)));
    }

    private static boolean crossedHardThreshold(ServerPlayer player, SeamlessThresholdDetector.ThresholdResult result)
    {
        return switch (result.kind())
        {
            case SURFACE_APPROACHING_ORBIT -> altitudeKm(player) >= Config.ORBIT_ENTRY_ALTITUDE_KM.get();
            case SPACE_APPROACHING_PLANET -> result.targetPlanet().map(p -> result.distanceOrHeight() <= p.approachRadius()).orElse(false);
            default -> false;
        };
    }

    private static double altitudeKm(ServerPlayer player)
    {
        ObserverState obs = ObserverStates.resolve(player.level(), player.position(), 0f);
        return obs.hasBody() ? obs.altitudeKm() : Double.MAX_VALUE;
    }
}