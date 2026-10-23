package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.Frame.FrameAPI;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameBox;
import cute.ame.pioneer.Core.Observer.ObserverState;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TransitionManager
{
    public enum State { IDLE, SWAPPING, SETTLING }

    private static final int SETTLE_TICKS = 10;
    private static final Map<UUID, PlayerTransition> PLAYER_STATES = new HashMap<>();
    private static final String HELD_TEXT = "ships and vehicles cannot change dimension yet";
    private static final FrameBox PROBE = new FrameBox();

    private static void tickIdle(ServerPlayer player, PlayerTransition pt)
    {
        if (!SeamlessThresholdDetector.shouldCheck(player)) return;

        SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
        if (result.kind() == SeamlessThresholdDetector.ThresholdKind.NONE || !crossedHardThreshold(player, result))
        {
            pt.held = pt.refused = false;
            return;
        }

        if (pt.refused) return;

        String aboard = aboard(player);
        if (aboard != null)
        {
            hold(player, pt, result, aboard);
            return;
        }

        Optional<ResourceKey<Level>> targetOpt = resolveTarget(player, result);
        if (targetOpt.isEmpty()) return;

        pt.kind = result.kind();
        pt.fromDim = player.level().dimension();
        pt.targetDim = targetOpt.get();
        pt.planet = result.targetPlanet().orElse(null);

        doSwap(player, pt);
    }

    @Nullable
    private static String aboard(ServerPlayer player)
    {
        if (player.getVehicle() != null) return "riding";
        if (Sable.HELPER.getTrackingSubLevel(player) != null) return "on a ship";

        return nearShip(player) ? "near a ship" : null;
    }

    private static boolean nearShip(ServerPlayer player)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(player.serverLevel());
        if (container == null) return false;

        double radius = Config.FRAME_CARRY_RADIUS.get();
        AABB b = player.getBoundingBox();
        PROBE.set(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
        for (ServerSubLevel ship : container.getAllSubLevels())
        {
            if (ship.isRemoved()) continue;

            BoundingBox3dc s = ship.boundingBox();
            if (PROBE.gap(s.minX(), s.minY(), s.minZ(), s.maxX(), s.maxY(), s.maxZ()) <= radius) return true;
        }

        return false;
    }

    private static void hold(ServerPlayer player, PlayerTransition pt, SeamlessThresholdDetector.ThresholdResult result, String aboard)
    {
        if (pt.held) return;

        pt.held = true;
        Pioneer.LOGGER.info("[Pioneer] {} not warped from {} ({}): {}, {}", player.getScoreboardName(), player.level().dimension().location(), result.kind(), aboard, HELD_TEXT);
    }

    public static void tick(ServerPlayer player)
    {
        PlayerTransition pt = PLAYER_STATES.computeIfAbsent(player.getUUID(), k -> new PlayerTransition());

        switch (pt.state)
        {
            case IDLE -> tickIdle(player, pt);
            case SWAPPING ->
            {
            }
            case SETTLING -> tickSettling(pt);
        }
    }

    private static void doSwap(ServerPlayer player, PlayerTransition pt)
    {
        pt.state = State.SWAPPING;

        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server != null ? server.getLevel(pt.targetDim) : null;
        if (targetLevel != null && warpThroughFrames(player, pt, targetLevel))
        {
            pt.state = State.SETTLING;
            pt.settleTicksRemaining = SETTLE_TICKS;
            return;
        }

        refuse(player, pt);
    }

    private static void refuse(ServerPlayer player, PlayerTransition pt)
    {
        Pioneer.LOGGER.warn("[Pioneer] {} not warped from {} to {}: the frame warp refused", player.getScoreboardName(), pt.fromDim.location(), pt.targetDim.location());
        resetToIdle(pt);
        pt.refused = true;
    }

    private static boolean warpThroughFrames(ServerPlayer player, PlayerTransition pt, ServerLevel targetLevel)
    {
        return switch (pt.kind)
        {
            case SURFACE_APPROACHING_ORBIT -> FrameAPI.warpToSpace(player, targetLevel);
            case SPACE_APPROACHING_PLANET ->
                pt.planet != null && FrameAPI.warpToSurface(player, targetLevel, pt.planet);
            default -> false;
        };
    }

    private static void resetToIdle(PlayerTransition pt)
    {
        pt.kind = null;
        pt.state = State.IDLE;
        pt.fromDim = null;
        pt.targetDim = null;
        pt.planet = null;
        pt.settleTicksRemaining = 0;
        pt.held = pt.refused = false;
    }

    private static void tickSettling(PlayerTransition pt)
    {
        if (--pt.settleTicksRemaining <= 0) resetToIdle(pt);
    }

    private static Optional<ResourceKey<Level>> resolveTarget(ServerPlayer player, SeamlessThresholdDetector.ThresholdResult result)
    {
        return switch (result.kind())
        {
            case SURFACE_APPROACHING_ORBIT -> orbitTarget(player);
            case SPACE_APPROACHING_PLANET ->
                result.targetPlanet().flatMap(planet -> planet.dimension().map(loc -> ResourceKey.create(Registries.DIMENSION, loc)));

            default -> Optional.empty();
        };
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

    private static Optional<ResourceKey<Level>> orbitTarget(ServerPlayer player)
    {
        var bindingOpt = PioneerAPI.getBindingForDimension(player.level().dimension());
        if (bindingOpt.isEmpty()) return Optional.empty();

        var systemOpt = PioneerAPI.getSolarSystem(bindingOpt.get().systemId());
        if (systemOpt.isEmpty()) return Optional.empty();

        var planetOpt = systemOpt.get().findById(bindingOpt.get().planetId());
        var spaceDimOpt = systemOpt.get().spaceDimension();
        if (planetOpt.isEmpty() || spaceDimOpt.isEmpty()) return Optional.empty();

        return Optional.of(ResourceKey.create(Registries.DIMENSION, spaceDimOpt.get()));
    }

    private static final class PlayerTransition
    {
        SeamlessThresholdDetector.ThresholdKind kind;
        State state = State.IDLE;
        ResourceKey<Level> fromDim;
        ResourceKey<Level> targetDim;
        PlanetDefinition planet;
        int settleTicksRemaining;
        boolean held, refused;
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
