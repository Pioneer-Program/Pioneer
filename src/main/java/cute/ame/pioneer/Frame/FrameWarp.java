package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.Frame.FrameError;
import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameLook;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Core.Frame.FrameQuat;
import cute.ame.pioneer.Core.Observer.CubeSurface;
import cute.ame.pioneer.Core.Observer.ObserverState;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.Core.Observer.PlanetCube;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

public final class FrameWarp
{
    public static final double REENTRY_RATIO = 0.75;

    private static final double CUBE_CORNER = 1.7320508075688772;
    private static final double HEIGHT_MARGIN = 8.0;

    private static final FramePose FROM = new FramePose();
    private static final FramePose TO = new FramePose();
    private static final double[] A = new double[3];
    private static final double[] B = new double[3];
    private static final double[] Q = new double[4];

    public static boolean toSpace(ServerPlayer player, ServerLevel space)
    {
        if (!Config.WARP_THROUGH_FRAMES.get()) return false;

        PlanetDefinition planet = ObserverStates.surfaceHost(player.level()).orElse(null);
        SolarSystemDefinition system = FrameBodies.systemOf(space.dimension());
        FrameBodies.Body body = planet == null || system == null ? null : FrameBodies.of(system, planet);
        if (body == null) return false;

        long now = space.getGameTime();
        FrameMotion motion = exit(planet, player.getX(), player.getEyeY(), player.getZ(), now, new FrameMotion());
        FROM.set(motion, body, true, now, 0.0);

        FrameManager manager = FrameManager.getOrCreate(space);
        FrameGrid grid = manager.grid();
        float yaw = player.getYRot(), pitch = player.getXRot();

        LocalFrame frame = manager.nearest(FROM.px, FROM.py, FROM.pz, now, Config.FRAME_JOIN_RADIUS.get(), A);
        if (frame != null && !(frame.fixed() && frame.parentName().equals(body.name()) && fits(space, grid, A[1], player.getEyeHeight())))
            frame = null;

        if (frame == null)
        {
            try
            {
                frame = manager.open(space, motion, body, true, true);
            }
            catch (FrameError e)
            {
                Pioneer.LOGGER.warn("frame warp of {} falls back to raw space: {}", player.getScoreboardName(), e.getMessage());
                return false;
            }

            A[0] = A[1] = A[2] = 0.0;
        }
        else
        {
            manager.poseOf(frame, now, 0.0, TO);
            FrameLook.direction(yaw, pitch, B);
            FROM.directionToSystem(B[0], B[1], B[2], B);
            TO.directionToLocal(B[0], B[1], B[2], B);
            yaw = FrameLook.yaw(B[0], B[2], yaw);
            pitch = FrameLook.pitch(B[0], B[1], B[2]);
        }

        int cell = frame.cell();
        FrameSync.reset(player, space);
        player.teleportTo(space, grid.centerX(cell) + A[0], grid.centerY() + A[1] - player.getEyeHeight(), grid.centerZ(cell) + A[2], Set.of(), yaw, pitch);
        Pioneer.LOGGER.debug("frame {} warped into #{} above {}", player.getScoreboardName(), frame.id, body.name());
        return true;
    }

    public static boolean toSurface(ServerPlayer player, ServerLevel surface, PlanetDefinition planet)
    {
        if (!Config.WARP_THROUGH_FRAMES.get()) return false;

        ServerLevel space = player.serverLevel();
        FrameManager manager = FrameManager.of(space);
        LocalFrame frame = manager == null ? null : manager.at(player.getX(), player.getZ());
        SolarSystemDefinition system = FrameBodies.systemOf(space.dimension());
        FrameBodies.Body body = frame == null || system == null ? null : FrameBodies.of(system, planet);
        if (body == null) return false;

        long now = space.getGameTime();
        FrameGrid grid = manager.grid();
        manager.poseOf(frame, now, 0.0, FROM);
        double[] to = new double[5];
        int face = landing(body, planet, FROM, now, player.getX() - grid.centerX(frame.cell()), player.getEyeY() - grid.centerY(), player.getZ() - grid.centerZ(frame.cell()), player.getYRot(), player.getXRot(), to);
        double x = to[0], z = to[2];
        double y = to[1] - player.getEyeHeight();
        float yaw = (float) to[3], pitch = (float) to[4];

        if (y < surface.getMaxBuildHeight())
        {
            int bx = Mth.floor(x), bz = Mth.floor(z);
            surface.getChunk(bx >> 4, bz >> 4);
            y = Math.max(y, surface.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) + 1.0);
        }

        player.teleportTo(surface, x, y, z, Set.of(), yaw, pitch);
        Pioneer.LOGGER.debug("frame {} warped from #{} to {} face {}", player.getScoreboardName(), frame.id, body.name(), face);
        return true;
    }

    public static FrameMotion exit(PlanetDefinition planet, double x, double eyeY, double z, long now, FrameMotion out)
    {
        ObserverState obs = ObserverState.fromSurfaceBlocks(planet, x, eyeY, z, ObserverState.Origin.SURFACE_BLOCKS);
        CubeSurface.orientation(obs.face(), Q);
        return out.set(now, obs.bodyKmX(), obs.bodyKmY(), obs.bodyKmZ(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).orient(Q[0], Q[1], Q[2], Q[3]).spin(0.0, 0.0, 0.0);
    }

    public static FrameMotion above(PlanetDefinition planet, int face, double u, double v, double altitudeKm, long now, FrameMotion out)
    {
        ObserverState obs = ObserverState.ofFace(planet, face, u, v, altitudeKm, ObserverState.Origin.ORBITAL_STATE);
        CubeSurface.orientation(face, Q);
        return out.set(now, obs.bodyKmX(), obs.bodyKmY(), obs.bodyKmZ(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).orient(Q[0], Q[1], Q[2], Q[3]).spin(0.0, 0.0, 0.0);
    }

    public static int landing(FrameBodies.Body body, PlanetDefinition planet, FramePose from, long now, double eyeX, double eyeY, double eyeZ, float yaw, float pitch, double[] out)
    {
        from.toSystem(eyeX, eyeY, eyeZ, A);
        toBody(body, now, A[0], A[1], A[2], B);

        double half = PlanetCube.halfSide(planet);
        int face = CubeSurface.fromPointKm(PlanetCube.halfExtentKm(planet), B[0], B[1], B[2], A);
        out[0] = PlanetCube.faceOriginX(planet, face) + Mth.clamp(A[0], -1.0, 1.0) * half;
        out[1] = PlanetCube.blockY(planet, A[2]);
        out[2] = Mth.clamp(A[1], -1.0, 1.0) * half;

        CubeSurface.orientation(face, Q);
        FrameQuat.conjugate(FrameQuat.mul(body.orientationAt(now, 0.0, new double[4]), Q, Q), Q);
        FrameLook.direction(yaw, pitch, B);
        from.directionToSystem(B[0], B[1], B[2], B);
        FrameQuat.rotate(Q, B[0], B[1], B[2], B);
        out[3] = FrameLook.yaw(B[0], B[2], yaw);
        out[4] = FrameLook.pitch(B[0], B[1], B[2]);
        return face;
    }

    public static double altitudeKm(FrameBodies.Body body, PlanetDefinition planet, long tick, double x, double y, double z, double limit)
    {
        double half = PlanetCube.halfExtentKm(planet);
        body.positionAt(tick, 0.0, B);
        double dx = x - B[0], dy = y - B[1], dz = z - B[2];
        double floor = Math.sqrt(dx * dx + dy * dy + dz * dz) / CUBE_CORNER - half;
        if (floor > limit) return floor;

        FrameQuat.conjugate(body.orientationAt(tick, 0.0, Q), Q);
        FrameQuat.rotate(Q, dx, dy, dz, B);
        return Math.max(Math.abs(B[0]), Math.max(Math.abs(B[1]), Math.abs(B[2]))) - half;
    }

    private static void toBody(FrameBodies.Body body, long tick, double x, double y, double z, double[] out)
    {
        body.positionAt(tick, 0.0, out);
        double dx = x - out[0], dy = y - out[1], dz = z - out[2];
        FrameQuat.conjugate(body.orientationAt(tick, 0.0, Q), Q);
        FrameQuat.rotate(Q, dx, dy, dz, out);
    }

    private static boolean fits(ServerLevel level, FrameGrid grid, double offsetY, double eyeHeight)
    {
        double feet = grid.centerY() + offsetY - eyeHeight;
        return feet > level.getMinBuildHeight() + HEIGHT_MARGIN && feet < level.getMaxBuildHeight() - HEIGHT_MARGIN;
    }
}
