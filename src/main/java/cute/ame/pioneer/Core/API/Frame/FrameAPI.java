package cute.ame.pioneer.Core.API.Frame;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Frame.FrameBodies;
import cute.ame.pioneer.Frame.FrameManager;
import cute.ame.pioneer.Frame.FrameTransfer;
import cute.ame.pioneer.Frame.FrameWarp;
import cute.ame.pioneer.Frame.LocalFrame;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.List;

public final class FrameAPI
{
    public static boolean supports(ServerLevel level)
    {
        return PioneerAPI.isSpaceDimension(level.dimension());
    }

    public static FrameGrid grid(ServerLevel level)
    {
        FrameManager manager = FrameManager.of(level);
        return manager != null ? manager.grid() : new FrameGrid(Config.FRAME_GRID_RADIUS.get(), Config.FRAME_CELL_SPACING.get(), Config.FRAME_GRID_Y.get());
    }

    public static List<LocalFrame> frames(ServerLevel level)
    {
        FrameManager manager = FrameManager.of(level);
        return manager == null ? List.of() : manager.frames();
    }

    public static void refresh(ServerLevel level)
    {
        FrameManager manager = FrameManager.of(level);
        if (manager != null) manager.refresh(level);
    }

    @Nullable
    public static LocalFrame frame(ServerLevel level, int id)
    {
        FrameManager manager = FrameManager.of(level);
        return manager == null ? null : manager.byId(id);
    }

    @Nullable
    public static LocalFrame frameAt(ServerLevel level, double x, double z)
    {
        FrameManager manager = FrameManager.of(level);
        return manager == null ? null : manager.at(x, z);
    }

    @Nullable
    public static LocalFrame frameInCell(ServerLevel level, int cell)
    {
        FrameManager manager = FrameManager.of(level);
        return manager == null ? null : manager.atCell(cell);
    }

    @Nullable
    public static LocalFrame frameOf(Entity entity)
    {
        return entity.level() instanceof ServerLevel level ? frameAt(level, entity.getX(), entity.getZ()) : null;
    }

    @Nullable
    public static LocalFrame frameOf(ServerLevel level, ServerSubLevel ship)
    {
        Vector3d at = ship.logicalPose().position();
        return frameAt(level, at.x, at.z);
    }

    public static int strays(ServerLevel level, int cell)
    {
        FrameManager manager = FrameManager.of(level);
        return manager == null ? 0 : manager.strays(cell);
    }

    public static FramePose pose(ServerLevel level, LocalFrame frame, double partial, FramePose out)
    {
        return owner(level).poseOf(frame, level.getGameTime(), partial, out);
    }

    public static FramePose pose(ServerLevel level, LocalFrame frame, long tick, double partial, FramePose out)
    {
        return owner(level).poseOf(frame, tick, partial, out);
    }

    public static double[] origin(ServerLevel level, LocalFrame frame, long tick, double partial, double[] out)
    {
        return owner(level).originOf(frame, tick, partial, out);
    }

    public static double[] velocity(ServerLevel level, LocalFrame frame, long tick, double[] out)
    {
        owner(level).velocityOf(frame, tick, out);
        out[0] *= 20.0;
        out[1] *= 20.0;
        out[2] *= 20.0;
        return out;
    }

    public static double[] toSystem(ServerLevel level, LocalFrame frame, double x, double y, double z, long tick, double partial, double[] out)
    {
        return owner(level).virtualOf(frame, x, y, z, tick, partial, out);
    }

    public static boolean systemPosition(Entity entity, double[] out)
    {
        out[0] = entity.getX();
        out[1] = entity.getY();
        out[2] = entity.getZ();
        if (!(entity.level() instanceof ServerLevel level)) return false;

        FrameManager manager = FrameManager.of(level);
        LocalFrame frame = manager == null ? null : manager.at(out[0], out[2]);
        if (frame == null) return false;

        manager.virtualOf(frame, out[0], out[1], out[2], level.getGameTime(), 0.0, out);
        return true;
    }

    @Nullable
    public static SolarSystemDefinition system(ServerLevel level)
    {
        return FrameBodies.systemOf(level.dimension());
    }

    public static List<FrameBodies.Body> bodies(ServerLevel level)
    {
        SolarSystemDefinition system = system(level);
        return system == null ? List.of() : FrameBodies.bodies(system);
    }

    @Nullable
    public static FrameBodies.Body body(ServerLevel level, String name)
    {
        SolarSystemDefinition system = system(level);
        return system == null ? null : FrameBodies.find(system, name);
    }

    public static String nearestBody(ServerLevel level, double x, double y, double z)
    {
        return FrameBodies.nearestText(system(level), x, y, z, level.getGameTime(), 0.0);
    }

    public static FrameResult create(ServerLevel level, @Nullable ServerPlayer source, ServerSubLevel ship, @Nullable FrameBodies.Body body)
    {
        return space(level).create(level, source, ship, body);
    }

    public static LocalFrame open(ServerLevel level, FrameMotion motion, @Nullable FrameParent parent, boolean fixed, boolean auto)
    {
        return space(level).open(level, motion, parent, fixed, auto);
    }

    public static void drop(ServerLevel level, LocalFrame frame)
    {
        owner(level).drop(level, frame);
    }

    public static FrameResult release(ServerLevel level, LocalFrame frame, boolean force)
    {
        return owner(level).release(level, frame, force);
    }

    public static FrameReport join(ServerLevel level, LocalFrame frame, ServerSubLevel ship)
    {
        return owner(level).join(level, frame, ship);
    }

    public static FrameReport recenter(ServerLevel level, LocalFrame frame)
    {
        return owner(level).recenter(level, frame);
    }

    public static void push(ServerLevel level, LocalFrame frame, double perSecondX, double perSecondY, double perSecondZ)
    {
        owner(level).push(frame, level.getGameTime(), perSecondX, perSecondY, perSecondZ);
    }

    public static void accelerate(ServerLevel level, LocalFrame frame, double perSecond2X, double perSecond2Y, double perSecond2Z)
    {
        owner(level).accelerate(frame, level.getGameTime(), perSecond2X, perSecond2Y, perSecond2Z);
    }

    public static void stop(ServerLevel level, LocalFrame frame)
    {
        owner(level).stop(frame, level.getGameTime());
    }

    public static void spin(ServerLevel level, LocalFrame frame, double radPerSecondX, double radPerSecondY, double radPerSecondZ)
    {
        owner(level).spin(frame, level.getGameTime(), radPerSecondX / 20.0, radPerSecondY / 20.0, radPerSecondZ / 20.0);
    }

    public static void unspin(ServerLevel level, LocalFrame frame)
    {
        owner(level).unspin(frame, level.getGameTime());
    }

    public static void orient(ServerLevel level, LocalFrame frame, double qx, double qy, double qz, double qw)
    {
        owner(level).orient(frame, level.getGameTime(), qx, qy, qz, qw);
    }

    public static void place(ServerLevel level, LocalFrame frame, double x, double y, double z)
    {
        owner(level).place(frame, level.getGameTime(), x, y, z);
    }

    public static void assign(ServerLevel level, LocalFrame frame, FrameMotion motion, @Nullable FrameParent parent, boolean fixed)
    {
        owner(level).assign(frame, motion, parent, fixed);
    }

    public static void reparent(ServerLevel level, LocalFrame frame, @Nullable FrameParent parent, boolean fixed)
    {
        owner(level).reparent(frame, level.getGameTime(), parent, fixed);
    }

    public static FrameMotion surfaceMotion(PlanetDefinition planet, int face, double u, double v, double altitudeKm, long tick, FrameMotion out)
    {
        return FrameWarp.above(planet, face, u, v, altitudeKm, tick, out);
    }

    public static double altitudeKm(FrameBodies.Body body, PlanetDefinition planet, long tick, double x, double y, double z, double limit)
    {
        return FrameWarp.altitudeKm(body, planet, tick, x, y, z, limit);
    }

    public static double reentryAltitudeKm()
    {
        return Config.ORBIT_ENTRY_ALTITUDE_KM.get() * FrameWarp.REENTRY_RATIO;
    }

    public static boolean warpToSpace(ServerPlayer player, ServerLevel space)
    {
        return FrameWarp.toSpace(player, space);
    }

    public static boolean warpToSurface(ServerPlayer player, ServerLevel surface, PlanetDefinition planet)
    {
        return FrameWarp.toSurface(player, surface, planet);
    }

    public static Vector3d velocity(ServerLevel level, ServerSubLevel ship, Vector3d dest)
    {
        return FrameTransfer.velocityOf(level, ship, dest);
    }

    public static String name(ServerSubLevel ship)
    {
        return FrameTransfer.shortId(ship);
    }

    private static FrameManager space(ServerLevel level)
    {
        if (!supports(level))
            throw new FrameError("Frames only exist in a space dimension, not " + level.dimension().location());

        return FrameManager.getOrCreate(level);
    }

    private static FrameManager owner(ServerLevel level)
    {
        FrameManager manager = FrameManager.of(level);
        if (manager == null) throw new FrameError("No frame in " + level.dimension().location());

        return manager;
    }
}
