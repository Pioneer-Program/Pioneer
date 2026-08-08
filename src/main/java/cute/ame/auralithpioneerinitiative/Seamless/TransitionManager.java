package cute.ame.auralithpioneerinitiative.Seamless;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Config;
import cute.ame.auralithpioneerinitiative.Seamless.Network.PreloadCancelPayload;
import cute.ame.auralithpioneerinitiative.Seamless.Network.PreloadDimensionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TransitionManager
{
    public enum State { IDLE, APPROACHING, PRELOADING, READY, SWAPPING, SETTLING }

    private static final class PlayerTransition
    {
        SeamlessThresholdDetector.ThresholdKind kind;
        State state = State.IDLE;
        ResourceKey<Level> fromDim;
        ResourceKey<Level> targetDim;
        BlockPos anchor;
        int settleTicksRemaining;
        int ticksInPreloading;
    }

    private static final int SETTLE_TICKS = 10;
    private static final int PRELOAD_RESEND_INTERVAL_TICKS = 10;

    private static final Map<UUID, PlayerTransition> PLAYER_STATES = new HashMap<>();

    public static void tick(ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        PlayerTransition pt = PLAYER_STATES.computeIfAbsent(uuid, k -> new PlayerTransition());

        switch (pt.state)
        {
            case IDLE -> tickIdle(player, pt);
            case APPROACHING -> tickApproaching(player, pt);
            case PRELOADING -> tickPreloading(player, pt);
            case READY -> tickReady(player, pt);
            case SWAPPING -> {  }
            case SETTLING -> tickSettling(player, pt);
        }
    }

    private static void tickIdle(ServerPlayer player, PlayerTransition pt)
    {
        if (!SeamlessThresholdDetector.shouldCheck(player)) return;

        SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
        if (result.kind() == SeamlessThresholdDetector.ThresholdKind.NONE) return;

        Optional<ResolvedTarget> targetOpt = resolveTarget(player, result);
        if (targetOpt.isEmpty()) return;

        ResolvedTarget target = targetOpt.get();
        pt.kind = result.kind();
        pt.fromDim = player.level().dimension();
        pt.targetDim = target.dimension();
        pt.anchor = target.anchor();
        pt.state = State.APPROACHING;

        Auralithpioneerinitiative.LOGGER.debug("[Auralith] {} APPROACHING {}", player.getScoreboardName(), pt.targetDim.location());
    }

    private static void tickApproaching(ServerPlayer player, PlayerTransition pt)
    {
        if (playerBackedOff(player, pt))
        {
            cancel(player, pt);
            return;
        }

        SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
        if (result.kind() == SeamlessThresholdDetector.ThresholdKind.NONE)
        {
            cancel(player, pt);
            return;
        }

        Optional<ResolvedTarget> targetOpt = resolveTarget(player, result);
        if (targetOpt.isEmpty()) { cancel(player, pt); return; }
        pt.anchor = targetOpt.get().anchor();

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel targetLevel = server.getLevel(pt.targetDim);
        if (targetLevel == null) { cancel(player, pt); return; }

        if (!SeamlessLevelRegistry.hasRoomFor(pt.targetDim))
        {
            return;
        }

        beginPreloading(player, pt, targetLevel);
    }

    private static void beginPreloading(ServerPlayer player, PlayerTransition pt, ServerLevel targetLevel)
    {
        pt.state = State.PRELOADING;
        pt.ticksInPreloading = 0;

        int radiusChunks = computeRadiusChunks(player, targetLevel);
        SeamlessPreloadManager.preload(player.getUUID(), targetLevel, pt.anchor, radiusChunks);

        PacketDistributor.sendToPlayer(player, new PreloadDimensionPayload(pt.fromDim, pt.targetDim, pt.anchor, targetLevel.dimensionTypeRegistration().unwrapKey().orElseThrow()));

        Auralithpioneerinitiative.LOGGER.debug("[Auralith] {} PRELOADING {} (radius={} chunks, anchor={})",
                player.getScoreboardName(), pt.targetDim.location(), radiusChunks, pt.anchor.toShortString());
    }

    private static void tickPreloading(ServerPlayer player, PlayerTransition pt)
    {
        if (playerBackedOff(player, pt))
        {
            cancel(player, pt);
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel targetLevel = server.getLevel(pt.targetDim);
        if (targetLevel == null) { cancel(player, pt); return; }

        if (player.tickCount % PRELOAD_RESEND_INTERVAL_TICKS == 0)
        {
            SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
            Optional<ResolvedTarget> targetOpt = resolveTarget(player, result);
            if (targetOpt.isPresent())
            {
                pt.anchor = targetOpt.get().anchor();
                int radiusChunks = computeRadiusChunks(player, targetLevel);
                SeamlessPreloadManager.preload(player.getUUID(), targetLevel, pt.anchor, radiusChunks);
            }
        }

        java.util.Set<net.minecraft.world.level.ChunkPos> forced =
                SeamlessPreloadManager.getForcedSnapshot(player.getUUID(), pt.targetDim);
        if (!forced.isEmpty())
        {
            SeamlessChunkStreamer.streamReadyChunks(player, targetLevel, forced);
        }

        if (isPreloadReady(player, targetLevel, pt.targetDim, pt.anchor))
        {
            pt.state = State.READY;
            Auralithpioneerinitiative.LOGGER.debug("[Auralith] {} READY for {}", player.getScoreboardName(), pt.targetDim.location());
        }

        if (crossedRealThreshold(player, pt))
        {
            doSwap(player, pt);
        }
    }

    private static void tickReady(ServerPlayer player, PlayerTransition pt)
    {
        if (playerBackedOff(player, pt))
        {
            cancel(player, pt);
            return;
        }

        if (crossedRealThreshold(player, pt))
        {
            doSwap(player, pt);
        }
    }

    private static void doSwap(ServerPlayer player, PlayerTransition pt)
    {
        pt.state = State.SWAPPING;

        MinecraftServer server = player.getServer();
        if (server == null) { pt.state = State.IDLE; return; }
        ServerLevel targetLevel = server.getLevel(pt.targetDim);
        if (targetLevel == null) { pt.state = State.IDLE; return; }

        BlockPos landing = pt.anchor;

        if (pt.kind == SeamlessThresholdDetector.ThresholdKind.SPACE_APPROACHING_PLANET)
        {

            targetLevel.getChunk(landing.getX() >> 4, landing.getZ() >> 4);
            int surfaceY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, landing.getX(), landing.getZ());
            landing = new BlockPos(landing.getX(), surfaceY + 1, landing.getZ());
        }

        player.teleportTo(targetLevel, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot());
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] {} SWAPPING {} -> {}", player.getScoreboardName(), pt.fromDim.location(), pt.targetDim.location());

        pt.state = State.SETTLING;
        pt.settleTicksRemaining = SETTLE_TICKS;
    }

    private static void tickSettling(ServerPlayer player, PlayerTransition pt)
    {
        pt.settleTicksRemaining--;
        if (pt.settleTicksRemaining > 0) return;

        MinecraftServer server = player.getServer();
        ServerLevel fromLevel = server != null ? server.getLevel(pt.fromDim) : null;
        if (fromLevel != null)
        {
            SeamlessPreloadManager.release(player.getUUID(), pt.fromDim, fromLevel);
        }

        SeamlessChunkStreamer.release(player.getUUID(), pt.fromDim);

        Auralithpioneerinitiative.LOGGER.debug("[Auralith] {} SETTLED in {}", player.getScoreboardName(), pt.targetDim.location());

        PacketDistributor.sendToPlayer(player, new cute.ame.auralithpioneerinitiative.Seamless.Network.TransitionCompletePayload(pt.fromDim, pt.targetDim));
        resetToIdle(pt);
    }

    private static void cancel(ServerPlayer player, PlayerTransition pt)
    {
        if (pt.state == State.PRELOADING || pt.state == State.READY)
        {
            MinecraftServer server = player.getServer();
            ServerLevel targetLevel = server != null ? server.getLevel(pt.targetDim) : null;
            if (targetLevel != null)
            {
                SeamlessPreloadManager.release(player.getUUID(), pt.targetDim, targetLevel);
            }
            SeamlessChunkStreamer.release(player.getUUID(), pt.targetDim);
            PacketDistributor.sendToPlayer(player, new PreloadCancelPayload(pt.targetDim));
            Auralithpioneerinitiative.LOGGER.debug("[Auralith] {} CANCELLED transition to {}", player.getScoreboardName(), pt.targetDim.location());
        }
        resetToIdle(pt);
    }

    private static void resetToIdle(PlayerTransition pt)
    {
        pt.kind = null;
        pt.state = State.IDLE;
        pt.fromDim = null;
        pt.targetDim = null;
        pt.anchor = null;
        pt.settleTicksRemaining = 0;
        pt.ticksInPreloading = 0;
    }

    public static void onPlayerDisconnect(ServerPlayer player, java.util.function.Function<ResourceKey<Level>, ServerLevel> levelLookup)
    {
        PlayerTransition pt = PLAYER_STATES.remove(player.getUUID());
        if (pt != null && pt.targetDim != null)
        {
            SeamlessPreloadManager.releaseAllForPlayer(player.getUUID(), levelLookup);
        }
        SeamlessChunkStreamer.releaseAllForPlayer(player.getUUID());
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
            case SURFACE_APPROACHING_ORBIT ->
            {
                var bindingOpt = cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI.getBindingForDimension(player.level().dimension());
                if (bindingOpt.isEmpty()) { yield Optional.empty(); }
                var binding = bindingOpt.get();
                var systemOpt = cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI.getSolarSystem(binding.systemId());
                if (systemOpt.isEmpty()) { yield Optional.empty(); }

                var planetOpt = systemOpt.get().findById(binding.planetId());
                if (planetOpt.isEmpty()) { yield Optional.empty(); }
                var planet = planetOpt.get();

                var spaceDimOpt = systemOpt.get().spaceDimension();
                if (spaceDimOpt.isEmpty()) { yield Optional.empty(); }

                ResourceKey<Level> spaceDim = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, spaceDimOpt.get());

                double[] pos = planet.currentWorldPosition(player.level().getGameTime());

                double clearance = (planet.size() * 0.5) + approachLandingMargin();
                BlockPos anchor = new BlockPos((int) pos[0], (int) (pos[1] + clearance), (int) pos[2]);
                yield Optional.of(new ResolvedTarget(spaceDim, anchor));
            }

            case SPACE_APPROACHING_PLANET -> result.targetPlanet().flatMap(planet ->
                    planet.dimension()
                            .map(surfaceLoc -> net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, surfaceLoc))

                            .map(surfaceDim -> new ResolvedTarget(surfaceDim, surfaceLandingColumn(player, planet))));

            default -> Optional.empty();
        };
    }

    private static final double SURFACE_LANDING_RADIUS = 200.0;

    private static double approachLandingMargin()
    {
        return Config.ORBIT_ENTRY_Y.get() - Config.SHOW_OWN_PLANET_START_Y.get();
    }

    public static BlockPos surfaceLandingColumn(ServerPlayer player, cute.ame.auralithpioneerinitiative.SkyPlanet.Data.PlanetDefinition planet)
    {
        double[] center = planet.currentWorldPosition(player.level().getGameTime());
        double dx = player.getX() - center[0];
        double dz = player.getZ() - center[2];

        double horizontalLen = Math.sqrt(dx * dx + dz * dz);
        double dirX, dirZ;
        if (horizontalLen < 1e-6)
        {

            dirX = 1.0;
            dirZ = 0.0;
        }
        else
        {
            dirX = dx / horizontalLen;
            dirZ = dz / horizontalLen;
        }

        int landingX = (int) Math.round(dirX * SURFACE_LANDING_RADIUS);
        int landingZ = (int) Math.round(dirZ * SURFACE_LANDING_RADIUS);

        return new BlockPos(landingX, 100, landingZ);
    }

    private static boolean playerBackedOff(ServerPlayer player, PlayerTransition pt)
    {
        SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
        return result.kind() == SeamlessThresholdDetector.ThresholdKind.NONE;
    }

    private static boolean crossedRealThreshold(ServerPlayer player, PlayerTransition pt)
    {
        return switch (lastResultKind(player))
        {
            case SURFACE_APPROACHING_ORBIT -> player.getY() >= Config.ORBIT_ENTRY_Y.get();
            case SPACE_APPROACHING_PLANET -> crossedPlanetHardThreshold(player, pt);
            default -> false;
        };
    }

    private static boolean crossedPlanetHardThreshold(ServerPlayer player, PlayerTransition pt)
    {
        SeamlessThresholdDetector.ThresholdResult result = SeamlessThresholdDetector.evaluate(player);
        if (result.kind() != SeamlessThresholdDetector.ThresholdKind.SPACE_APPROACHING_PLANET) return false;
        if (result.targetPlanet().isEmpty()) return false;

        var planet = result.targetPlanet().get();
        return result.distanceOrHeight() <= planet.approachRadius();
    }

    private static SeamlessThresholdDetector.ThresholdKind lastResultKind(ServerPlayer player)
    {
        return SeamlessThresholdDetector.evaluate(player).kind();
    }

    private static int computeRadiusChunks(ServerPlayer player, ServerLevel targetLevel)
    {
        int serverViewDistance = player.getServer() != null ? player.getServer().getPlayerList().getViewDistance() : 10;
        int clientViewDistance = player.requestedViewDistance();
        int effectiveChunks = Math.max(4, Math.min(serverViewDistance, clientViewDistance));
        return effectiveChunks;
    }

    private static final int PRELOAD_STALL_TICKS_BEFORE_FALLBACK = 100;

    private static boolean isPreloadReady(ServerPlayer player, ServerLevel targetLevel, ResourceKey<Level> targetDim, BlockPos anchor)
    {
        if (SeamlessPreloadManager.isFullyLoaded(player.getUUID(), targetDim, targetLevel))
        {
            return true;
        }

        PlayerTransition pt = PLAYER_STATES.get(player.getUUID());
        if (pt != null)
        {
            pt.ticksInPreloading++;
            if (pt.ticksInPreloading >= PRELOAD_STALL_TICKS_BEFORE_FALLBACK)
            {
                Auralithpioneerinitiative.LOGGER.warn("[Auralith] {} preload to {} stalled after {} ticks, falling back to anchor-only readiness",
                        player.getScoreboardName(), targetDim.location(), pt.ticksInPreloading);
                return targetLevel.hasChunk(anchor.getX() >> 4, anchor.getZ() >> 4);
            }
        }
        return false;
    }
}