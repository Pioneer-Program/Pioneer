package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.Frame.FrameError;
import cute.ame.pioneer.Core.API.Frame.FrameEvent;
import cute.ame.pioneer.Core.API.Frame.FrameReport;
import cute.ame.pioneer.Core.API.Frame.FrameResult;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameBox;
import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Core.Frame.FrameQuat;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2i;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FrameManager extends SavedData
{
    public static final String NAME = "pioneer_frames";

    private static final SavedData.Factory<FrameManager> FACTORY = new SavedData.Factory<>(FrameManager::fromConfig, FrameManager::load, null);
    private final ArrayList<LocalFrame> frames = new ArrayList<>(8);
    private final List<LocalFrame> framesView = Collections.unmodifiableList(frames);
    private final FramePose pose = new FramePose();
    private FrameGrid grid;
    private LocalFrame[] byCell;
    private int[] strays;
    private int nextId = 1;
    @Nullable
    private ServerLevel level;

    private FrameManager(FrameGrid grid)
    {
        setGrid(grid);
    }

    private static FrameManager fromConfig()
    {
        return new FrameManager(configGrid());
    }

    private static FrameGrid configGrid()
    {
        FrameGrid grid = new FrameGrid(Config.FRAME_GRID_RADIUS.get(), Config.FRAME_CELL_SPACING.get(), Config.FRAME_GRID_Y.get());
        if (!grid.precise())
            Pioneer.LOGGER.error("frame invalid grid config (radius {}, spacing {}, y {})", grid.radius(), grid.spacing(), grid.y());

        return grid;
    }

    @Nullable
    public static FrameManager of(ServerLevel level)
    {
        return PioneerAPI.isSpaceDimension(level.dimension()) ? get(level) : null;
    }

    @Nullable
    public static FrameManager get(ServerLevel level)
    {
        FrameManager manager = level.getDataStorage().get(FACTORY, NAME);
        if (manager != null) manager.level = level;

        return manager;
    }

    private static List<Entity> entitiesNear(ServerLevel level, ServerSubLevel ship)
    {
        double radius = Config.FRAME_CARRY_RADIUS.get();
        BoundingBox3dc s = ship.boundingBox();
        AABB area = new AABB(s.minX(), s.minY(), s.minZ(), s.maxX(), s.maxY(), s.maxZ()).inflate(radius);
        return level.getEntities((Entity) null, area, e -> !(e instanceof Player) && !e.isPassenger() && e.isAlive());
    }

    public static FrameManager getOrCreate(ServerLevel level)
    {
        FrameManager manager = level.getDataStorage().computeIfAbsent(FACTORY, NAME);
        manager.level = level;
        manager.adoptConfigGrid();
        return manager;
    }

    public static FrameManager load(CompoundTag tag, HolderLookup.Provider registries)
    {
        FrameManager manager = new FrameManager(new FrameGrid(tag.getInt("radius"), Math.max(tag.getInt("spacing"), 1), tag.getInt("y")));
        manager.nextId = Math.max(tag.getInt("next"), 1);

        ListTag list = tag.getList("frames", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag t = list.getCompound(i);
            int cell = manager.grid.cell(t.getInt("ix"), t.getInt("iz"));
            int id = t.getInt("id");
            if (cell < 0 || manager.byCell[cell] != null)
            {
                Pioneer.LOGGER.error("frame dropped frame #{}: cell ({},{}) is invalid or taken", id, t.getInt("ix"), t.getInt("iz"));
                continue;
            }

            LocalFrame f = new LocalFrame(id, cell);
            f.motion.set(t.getLong("epoch"), t.getDouble("px"), t.getDouble("py"), t.getDouble("pz"), t.getDouble("vx"), t.getDouble("vy"), t.getDouble("vz"), t.getDouble("ax"), t.getDouble("ay"), t.getDouble("az"));
            if (t.contains("qw"))
            {
                double[] q = FrameQuat.normalize(new double[]{t.getDouble("qx"), t.getDouble("qy"), t.getDouble("qz"), t.getDouble("qw")});
                f.motion.orient(q[0], q[1], q[2], q[3]).spin(t.getDouble("wx"), t.getDouble("wy"), t.getDouble("wz"));
            }

            f.attach(t.getString("parent"), t.getBoolean("fixed"));
            f.auto = t.getBoolean("auto");
            f.absent = t.getInt("absent");

            manager.frames.add(f);
            manager.byCell[cell] = f;
            manager.nextId = Math.max(manager.nextId, id + 1);
        }

        if (!manager.frames.isEmpty() && !manager.grid.sameAs(Config.FRAME_GRID_RADIUS.get(), Config.FRAME_CELL_SPACING.get(), Config.FRAME_GRID_Y.get()))
            Pioneer.LOGGER.warn("frame grid config changed, keeping saved grid (radius {}, spacing {}, y {}) until all frames are released", manager.grid.radius(), manager.grid.spacing(), manager.grid.y());

        return manager;
    }

    private void adoptConfigGrid()
    {
        if (!frames.isEmpty()) return;

        int radius = Config.FRAME_GRID_RADIUS.get(), spacing = Config.FRAME_CELL_SPACING.get(), y = Config.FRAME_GRID_Y.get();
        if (!grid.sameAs(radius, spacing, y)) setGrid(configGrid());
    }

    public FrameGrid grid()
    {
        return grid;
    }

    public List<LocalFrame> frames()
    {
        return framesView;
    }

    @Nullable
    public LocalFrame byId(int id)
    {
        for (LocalFrame f : frames) if (f.id == id) return f;
        return null;
    }

    @Nullable
    public LocalFrame atCell(int cell)
    {
        return cell < 0 ? null : byCell[cell];
    }

    @Nullable
    public LocalFrame at(double x, double z)
    {
        return atCell(grid.cellAt(x, z));
    }

    public int strays(int cell)
    {
        return cell < 0 ? 0 : strays[cell];
    }

    public double centerX(LocalFrame f)
    {
        return grid.centerX(f.cell);
    }

    public double centerY()
    {
        return grid.centerY();
    }

    public double centerZ(LocalFrame f)
    {
        return grid.centerZ(f.cell);
    }

    private void setGrid(FrameGrid grid)
    {
        this.grid = grid;
        this.byCell = new LocalFrame[grid.cellCount()];
        this.strays = new int[grid.cellCount()];
    }

    @Nullable
    private SolarSystemDefinition system()
    {
        return level == null ? null : FrameBodies.systemOf(level.dimension());
    }

    public FramePose poseOf(LocalFrame f, long tick, double partial, FramePose out)
    {
        return out.set(f.motion, f.parent(system()), f.fixed, tick, partial);
    }

    public double[] originOf(LocalFrame f, long tick, double partial, double[] out)
    {
        return poseOf(f, tick, partial, pose).toSystem(0.0, 0.0, 0.0, out);
    }

    public void refresh(ServerLevel level)
    {
        for (LocalFrame f : frames) f.clearMembers();

        Arrays.fill(strays, 0);
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null)
        {
            for (ServerSubLevel subLevel : container.getAllSubLevels())
            {
                if (subLevel.isRemoved()) continue;

                Vector3d p = subLevel.logicalPose().position();
                int cell = grid.cellAt(p.x, p.z);
                if (cell < 0) continue;

                LocalFrame f = byCell[cell];
                if (f != null) f.addMember(subLevel, FrameTransfer.massOf(subLevel));
                else strays[cell]++;
            }
        }

        for (ServerPlayer player : level.players())
        {
            int cell = grid.cellAt(player.getX(), player.getZ());
            if (cell < 0) continue;

            LocalFrame f = byCell[cell];
            if (f != null) f.addPlayer(player);
            else strays[cell]++;
        }
    }

    public double[] velocityOf(LocalFrame f, long tick, double[] out)
    {
        return pose.velocity(f.motion, f.parent(system()), f.fixed, tick, 0.0, out);
    }

    public double[] virtualOf(LocalFrame f, double x, double y, double z, long tick, double partial, double[] out)
    {
        return poseOf(f, tick, partial, pose).toSystem(x - grid.centerX(f.cell), y - grid.centerY(), z - grid.centerZ(f.cell), out);
    }

    public FrameResult create(ServerLevel level, @Nullable ServerPlayer source, ServerSubLevel ship, @Nullable FrameBodies.Body body)
    {
        adoptConfigGrid();
        refresh(level);
        if (ship.isRemoved()) throw new FrameError("Ship was removed");

        ship.updateBoundingBox();
        Vector3d anchor = new Vector3d(ship.logicalPose().position());
        LocalFrame existing = at(anchor.x, anchor.z);
        if (existing != null) throw new FrameError("Ship is already in frame #" + existing.id);

        LocalFrame own = source == null ? null : at(source.getX(), source.getZ());
        if (own != null) throw new FrameError("You are in frame #" + own.id + ", release it first");

        List<ServerSubLevel> subLevels = List.of(ship);
        List<ServerPlayer> players = crewOf(level, source, ship);
        List<Entity> entities = entitiesNear(level, ship);
        int cell = grid.nearestFree(c -> byCell[c] == null && strays[c] == carriedIn(c, anchor, players));
        if (cell < 0) throw new FrameError("No free cell (" + frames.size() + "/" + grid.cellCount() + " used)");

        long now = level.getGameTime();
        Vector3d shipVelocity = FrameTransfer.velocityOf(level, ship, new Vector3d());
        LocalFrame frame = new LocalFrame(nextId++, cell);
        if (body == null)
            frame.motion.set(now, anchor.x, anchor.y, anchor.z, shipVelocity.x / 20.0, shipVelocity.y / 20.0, shipVelocity.z / 20.0, 0.0, 0.0, 0.0);
        else
        {
            double[] p = body.parkingPoint(anchor.x, anchor.y, anchor.z, now, new double[3]);
            double[] b = body.positionAt(now, 0.0, new double[3]);
            frame.motion.set(now, p[0] - b[0], p[1] - b[1], p[2] - b[2], 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            frame.attach(body.name(), false);
        }

        frames.add(frame);
        byCell[cell] = frame;
        setDirty();
        FrameSync.upsert(level, this, frame);
        FrameReport report = FrameTransfer.carry(level, subLevels, players, entities, grid.centerX(cell) - anchor.x, grid.centerY() - anchor.y, grid.centerZ(cell) - anchor.z, -shipVelocity.x, -shipVelocity.y, -shipVelocity.z);

        refresh(level);
        Pioneer.LOGGER.info("frame created #{} at ({},{})", frame.id, grid.ix(cell), grid.iz(cell));
        NeoForge.EVENT_BUS.post(new FrameEvent.Created(level, frame));
        return new FrameResult(frame, report);
    }

    public LocalFrame open(ServerLevel level, FrameMotion motion, @Nullable FrameParent parent, boolean fixed, boolean auto)
    {
        adoptConfigGrid();
        refresh(level);
        int cell = grid.nearestFree(c -> byCell[c] == null && strays[c] == 0);
        if (cell < 0) throw new FrameError("No free cell (" + frames.size() + "/" + grid.cellCount() + " used)");

        LocalFrame frame = new LocalFrame(nextId++, cell);
        frame.motion.set(motion);
        frame.attach(parent == null ? "" : parent.name(), fixed);
        frame.auto = auto;

        frames.add(frame);
        byCell[cell] = frame;
        setDirty();
        FrameSync.upsert(level, this, frame);
        Pioneer.LOGGER.info("frame opened #{} at ({},{})", frame.id, grid.ix(cell), grid.iz(cell));
        NeoForge.EVENT_BUS.post(new FrameEvent.Created(level, frame));
        return frame;
    }

    public void drop(ServerLevel level, LocalFrame frame)
    {
        refresh(level);
        if (!frame.members.isEmpty() || !frame.players.isEmpty())
            throw new FrameError("Frame #" + frame.id + " is not empty, release it instead");

        remove(level, frame);
    }

    public int sweep(ServerLevel level)
    {
        if (frames.isEmpty()) return 0;

        refresh(level);
        int dropped = 0;
        for (int i = frames.size() - 1; i >= 0; i--)
        {
            LocalFrame f = frames.get(i);
            if (!f.auto || f.absent > 0 || !f.members.isEmpty() || !f.players.isEmpty()) continue;

            remove(level, f);
            dropped++;
        }
        return dropped;
    }

    private void remove(ServerLevel level, LocalFrame frame)
    {
        int cell = frame.cell;
        if (byCell[cell] != frame) return;

        frames.remove(frame);
        byCell[cell] = null;
        frame.clearMembers();
        setDirty();
        FrameSync.remove(level, this, cell);
        Pioneer.LOGGER.info("frame dropped #{} from ({},{})", frame.id, grid.ix(cell), grid.iz(cell));
        NeoForge.EVENT_BUS.post(new FrameEvent.Released(level, frame));
    }

    public void absent(LocalFrame frame, int delta)
    {
        frame.absent = Math.max(0, frame.absent + delta);
        setDirty();
    }

    @Nullable
    public LocalFrame nearest(double x, double y, double z, long tick, double radius, double[] offsetOut)
    {
        LocalFrame best = null;
        double bestD = radius * radius;
        double[] o = new double[3];
        for (LocalFrame f : frames)
        {
            poseOf(f, tick, 0.0, pose).toLocal(x, y, z, o);
            double d = o[0] * o[0] + o[1] * o[1] + o[2] * o[2];
            if (d > bestD) continue;

            bestD = d;
            best = f;
            offsetOut[0] = o[0];
            offsetOut[1] = o[1];
            offsetOut[2] = o[2];
        }
        return best;
    }

    private static boolean overlapsPlotGrid(SubLevelContainer container, double minX, double minZ, double maxX, double maxZ)
    {
        int shift = container.getLogPlotSize() + SectionPos.SECTION_BITS, side = 1 << container.getLogSideLength();
        Vector2i origin = container.getOrigin();
        return (Mth.floor(maxX) >> shift) >= origin.x && (Mth.floor(minX) >> shift) < origin.x + side
            && (Mth.floor(maxZ) >> shift) >= origin.y && (Mth.floor(minZ) >> shift) < origin.y + side;
    }

    public FrameReport join(ServerLevel level, LocalFrame frame, ServerSubLevel ship)
    {
        refresh(level);
        if (ship.isRemoved()) throw new FrameError("Ship was removed");
        if (byCell[frame.cell] != frame) throw new FrameError("Frame #" + frame.id + " no longer exists");

        ship.updateBoundingBox();
        Vector3d at = new Vector3d(ship.logicalPose().position());
        LocalFrame existing = at(at.x, at.z);
        if (existing != null) throw new FrameError("Ship is already in frame #" + existing.id);

        long now = level.getGameTime();
        if (poseOf(frame, now, 0.0, pose).rotated)
            throw new FrameError("Frame #" + frame.id + " is rotated, a ship cannot join it yet");

        int cell = frame.cell;
        double[] o = pose.toLocal(at.x, at.y, at.z, new double[3]);
        double dx = grid.centerX(cell) + o[0] - at.x, dy = grid.centerY() + o[1] - at.y, dz = grid.centerZ(cell) + o[2] - at.z;
        double half = grid.halfCell();
        BoundingBox3dc b = ship.boundingBox();
        if (b.minX() - at.x + o[0] < -half || b.maxX() - at.x + o[0] > half || b.minZ() - at.z + o[2] < -half || b.maxZ() - at.z + o[2] > half)
            throw new FrameError(String.format(Locale.ROOT, "Ship is %.0f from frame #%d, outside its cell (half %.0f)", Math.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]), frame.id, half));
        if (b.minY() + dy < level.getMinBuildHeight() || b.maxY() + dy > level.getMaxBuildHeight())
            throw new FrameError("Ship would land outside the height limits of frame #" + frame.id);

        double[] v = velocityOf(frame, now, new double[3]);
        FrameReport report = FrameTransfer.carry(level, List.of(ship), crewOf(level, null, ship), entitiesNear(level, ship), dx, dy, dz, -v[0] * 20.0, -v[1] * 20.0, -v[2] * 20.0);

        refresh(level);
        Pioneer.LOGGER.info("frame {} joined #{}", FrameTransfer.shortId(ship), frame.id);
        NeoForge.EVENT_BUS.post(new FrameEvent.Joined(level, frame, ship));
        return report;
    }

    public FrameReport recenter(ServerLevel level, LocalFrame frame)
    {
        refresh(level);
        ServerSubLevel anchor = frame.anchor;
        if (anchor == null) throw new FrameError("Frame #" + frame.id + " has no ship to center on");

        int cell = frame.cell;
        Vector3d at = anchor.logicalPose().position();
        double cx = grid.centerX(cell), cz = grid.centerZ(cell), half = grid.halfCell();
        double ox = at.x - cx, oy = at.y - grid.centerY(), oz = at.z - cz;
        FrameBox box = frame.box;
        if (box.minX - ox < cx - half || box.maxX - ox > cx + half || box.minZ - oz < cz - half || box.maxZ - oz > cz + half)
            throw new FrameError("Frame #" + frame.id + " content would leave its cell");
        if (box.minY - oy < level.getMinBuildHeight() || box.maxY - oy > level.getMaxBuildHeight())
            throw new FrameError("Frame #" + frame.id + " content would leave the height limits");

        Vector3d u = FrameTransfer.velocityOf(level, anchor, new Vector3d());
        frame.motion.recenter(level.getGameTime(), ox, oy, oz, u.x / 20.0, u.y / 20.0, u.z / 20.0);
        changed(frame, FrameEvent.Cause.RECENTER);
        FrameReport report = FrameTransfer.carry(level, List.copyOf(frame.members), List.copyOf(frame.players), entitiesInCell(level, cell), -ox, -oy, -oz, -u.x, -u.y, -u.z);

        refresh(level);
        return report;
    }

    public void push(LocalFrame frame, long now, double dvx, double dvy, double dvz)
    {
        frame.motion.rebase(now).addVelocity(dvx / 20.0, dvy / 20.0, dvz / 20.0);
        changed(frame, FrameEvent.Cause.MOTION);
    }

    public void accelerate(LocalFrame frame, long now, double ax, double ay, double az)
    {
        frame.motion.rebase(now).accelerate(ax / 400.0, ay / 400.0, az / 400.0);
        changed(frame, FrameEvent.Cause.MOTION);
    }

    public void stop(LocalFrame frame, long now)
    {
        FrameMotion m = frame.motion.rebase(now);
        m.set(now, m.px, m.py, m.pz, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        changed(frame, FrameEvent.Cause.MOTION);
    }

    public void spin(LocalFrame frame, long now, double wx, double wy, double wz)
    {
        frame.motion.rebase(now).spin(wx, wy, wz);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void unspin(LocalFrame frame, long now)
    {
        frame.motion.rebase(now).spin(0.0, 0.0, 0.0).orient(0.0, 0.0, 0.0, 1.0);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void reparent(LocalFrame frame, long now, @Nullable FrameParent to, boolean fixed)
    {
        pose.reparent(frame.motion, frame.parent(system()), frame.fixed, to, fixed, now);
        frame.attach(to == null ? "" : to.name(), fixed);
        changed(frame, FrameEvent.Cause.PARENT);
    }

    public void orient(LocalFrame frame, long now, double qx, double qy, double qz, double qw)
    {
        double[] q = FrameQuat.normalize(new double[]{qx, qy, qz, qw});
        frame.motion.rebase(now).orient(q[0], q[1], q[2], q[3]);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void assign(LocalFrame frame, FrameMotion motion, @Nullable FrameParent parent, boolean fixed)
    {
        double[] q = FrameQuat.normalize(new double[]{motion.qx, motion.qy, motion.qz, motion.qw});
        frame.motion.set(motion).orient(q[0], q[1], q[2], q[3]);
        frame.attach(parent == null ? "" : parent.name(), fixed);
        changed(frame, FrameEvent.Cause.STATE);
    }

    public void place(LocalFrame frame, long now, double x, double y, double z)
    {
        FrameParent parent = frame.parent(system());
        FrameMotion m = pose.reparent(frame.motion, parent, frame.fixed, null, false, now);
        m.translate(x - m.px, y - m.py, z - m.pz);
        pose.reparent(m, null, false, parent, frame.fixed, now);
        changed(frame, FrameEvent.Cause.POSITION);
    }

    private void changed(LocalFrame frame, FrameEvent.Cause cause)
    {
        setDirty();
        if (level == null) return;

        FrameSync.upsert(level, this, frame);
        NeoForge.EVENT_BUS.post(new FrameEvent.Changed(level, frame, cause));
    }

    private List<Entity> entitiesInCell(ServerLevel level, int cell)
    {
        double half = grid.halfCell();
        double cx = grid.centerX(cell), cz = grid.centerZ(cell);
        AABB area = new AABB(cx - half, level.getMinBuildHeight(), cz - half, cx + half, level.getMaxBuildHeight(), cz + half);
        return level.getEntities((Entity) null, area, e -> !(e instanceof Player) && !e.isPassenger() && e.isAlive());
    }

    public FrameResult release(ServerLevel level, LocalFrame frame, boolean force)
    {
        refresh(level);

        long now = level.getGameTime();
        FrameMotion m = frame.motion;
        if (frame.fixed)
            throw new FrameError("Frame #" + frame.id + " is fixed to " + frame.parentName + ", detach it first");
        if (m.rotated()) throw new FrameError("Frame #" + frame.id + " is rotated, unspin it first");

        double[] p = originOf(frame, now, 0.0, new double[3]);
        double[] v = velocityOf(frame, now, new double[3]);
        double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) * 20.0;
        double limit = Config.FRAME_RELEASE_MAX_SPEED.get();
        if (!force && speed > limit)
            throw new FrameError(String.format(Locale.ROOT, "Frame #%d is too fast (%.1f u/s, max %.1f)", frame.id, speed, limit));

        int cell = frame.cell;
        double dx = p[0] - grid.centerX(cell), dy = p[1] - grid.centerY(), dz = p[2] - grid.centerZ(cell);

        FrameBox box = frame.box;
        if (!box.isEmpty())
        {
            double minY = box.minY + dy, maxY = box.maxY + dy;
            if (minY < level.getMinBuildHeight() || maxY > level.getMaxBuildHeight())
                throw new FrameError("Frame #" + frame.id + " would land outside the height limits");

            double minX = box.minX + dx, maxX = box.maxX + dx, minZ = box.minZ + dz, maxZ = box.maxZ + dz;
            double extent = grid.extent();
            if (maxX > -extent && minX < extent && maxZ > -extent && minZ < extent)
                throw new FrameError("Frame #" + frame.id + " would land inside the frame grid, push it out first");

            if (!level.getWorldBorder().isWithinBounds(minX, minZ) || !level.getWorldBorder().isWithinBounds(maxX, maxZ))
                throw new FrameError("Frame #" + frame.id + " would land outside the world border");

            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container != null && overlapsPlotGrid(container, minX, minZ, maxX, maxZ))
                throw new FrameError("Frame #" + frame.id + " would land in the Sable plot grid");
        }

        List<ServerSubLevel> subLevels = List.copyOf(frame.members);
        List<ServerPlayer> players = List.copyOf(frame.players);
        List<Entity> entities = entitiesInCell(level, cell);
        FrameReport report = FrameTransfer.carry(level, subLevels, players, entities, dx, dy, dz, v[0] * 20.0, v[1] * 20.0, v[2] * 20.0);

        frames.remove(frame);
        byCell[cell] = null;
        frame.clearMembers();
        setDirty();
        FrameSync.remove(level, this, cell);
        Pioneer.LOGGER.info("frame released #{} from ({},{})", frame.id, grid.ix(cell), grid.iz(cell));
        NeoForge.EVENT_BUS.post(new FrameEvent.Released(level, frame));
        return new FrameResult(frame, report);
    }

    private int carriedIn(int cell, Vector3d anchor, List<ServerPlayer> players)
    {
        int n = grid.cellAt(anchor.x, anchor.z) == cell ? 1 : 0;
        for (ServerPlayer player : players) if (grid.cellAt(player.getX(), player.getZ()) == cell) n++;
        return n;
    }

    private List<ServerPlayer> crewOf(ServerLevel level, @Nullable ServerPlayer source, ServerSubLevel ship)
    {
        double radius = Config.FRAME_CARRY_RADIUS.get();
        BoundingBox3dc s = ship.boundingBox();
        FrameBox probe = new FrameBox();
        ArrayList<ServerPlayer> out = new ArrayList<>(2);
        for (ServerPlayer player : level.players())
        {
            if (player != source && at(player.getX(), player.getZ()) != null) continue;

            boolean crew = player == source || Sable.HELPER.getTrackingSubLevel(player) == ship || Sable.HELPER.getVehicleSubLevel(player) == ship;
            if (!crew)
            {
                AABB b = player.getBoundingBox();
                crew = probe.set(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ).gap(s.minX(), s.minY(), s.minZ(), s.maxX(), s.maxY(), s.maxZ()) <= radius;
            }

            if (crew) out.add(player);
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putInt("radius", grid.radius());
        tag.putInt("spacing", grid.spacing());
        tag.putInt("y", grid.y());
        tag.putInt("next", nextId);

        ListTag list = new ListTag();
        for (LocalFrame f : frames)
        {
            FrameMotion m = f.motion;
            CompoundTag t = new CompoundTag();
            t.putInt("id", f.id);
            t.putInt("ix", grid.ix(f.cell));
            t.putInt("iz", grid.iz(f.cell));
            t.putLong("epoch", m.epoch);
            t.putDouble("px", m.px);
            t.putDouble("py", m.py);
            t.putDouble("pz", m.pz);
            t.putDouble("vx", m.vx);
            t.putDouble("vy", m.vy);
            t.putDouble("vz", m.vz);
            t.putDouble("ax", m.ax);
            t.putDouble("ay", m.ay);
            t.putDouble("az", m.az);
            if (m.rotated())
            {
                t.putDouble("qx", m.qx);
                t.putDouble("qy", m.qy);
                t.putDouble("qz", m.qz);
                t.putDouble("qw", m.qw);
                t.putDouble("wx", m.wx);
                t.putDouble("wy", m.wy);
                t.putDouble("wz", m.wz);
            }

            if (!f.parentName.isEmpty())
            {
                t.putString("parent", f.parentName);
                t.putBoolean("fixed", f.fixed);
            }

            if (f.auto)
            {
                t.putBoolean("auto", true);
                t.putInt("absent", f.absent);
            }

            list.add(t);
        }
        tag.put("frames", list);
        return tag;
    }
}
