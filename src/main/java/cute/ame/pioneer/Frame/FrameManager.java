package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.Frame.FrameError;
import cute.ame.pioneer.Core.API.Frame.FrameEvent;
import cute.ame.pioneer.Core.API.Frame.FrameMaintenance;
import cute.ame.pioneer.Core.API.Frame.FrameReport;
import cute.ame.pioneer.Core.API.Frame.FrameResult;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameBox;
import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Core.Frame.FrameQuat;
import cute.ame.pioneer.Core.Frame.FrameShift;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
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
import java.util.UUID;

public final class FrameManager extends SavedData
{
    public static final String NAME = "pioneer_frames";

    private static final double TICKS_PER_SECOND = 20.0;
    private static final long FORGET_TICKS = 200L;
    private static final int WAIT_NONE = 0, WAIT_CELL = 1, WAIT_HEIGHT = 2, WAIT_DORMANT = 3;
    private static final String STAMP = "pioneer_frame_cell";
    private static final int ADOPT_NONE = 0;

    private static final SavedData.Factory<FrameManager> FACTORY = new SavedData.Factory<>(FrameManager::fromConfig, FrameManager::load, null);
    private final ArrayList<LocalFrame> frames = new ArrayList<>(8);
    private final List<LocalFrame> framesView = Collections.unmodifiableList(frames);
    private final FramePose pose = new FramePose();
    private final FrameMaintenance passReport = new FrameMaintenance();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final Vector3d linear = new Vector3d();
    private final FrameGroups groups = new FrameGroups();
    private final FrameShift stampShift = new FrameShift();
    private final ArrayList<ServerSubLevel> strayShips = new ArrayList<>(4);
    private final ArrayList<ServerPlayer> strayPlayers = new ArrayList<>(2);
    private double centerX, centerY, centerZ, driftX, driftY, driftZ;
    private int adoptWaitKey;
    private String adoptWaitText = "";
    private final double[] stampPoint = new double[4];
    private FrameShift[] cellShift;
    private long shiftEpoch;
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
        if (!PioneerAPI.isSpaceDimension(level.dimension())) return null;

        FrameManager manager = level.getDataStorage().get(FACTORY, NAME);
        if (manager != null) manager.level = level;

        return manager;
    }

    private static List<Entity> entitiesNear(ServerLevel level, List<ServerSubLevel> ships)
    {
        if (ships.size() == 1) return entitiesNear(level, ships.get(0));

        List<Entity> all = new ArrayList<>();
        for (int i = 0, n = ships.size(); i < n; ++i)
        {
            List<Entity> near = entitiesNear(level, ships.get(i));
            for (int k = 0, m = near.size(); k < m; ++k) if (!all.contains(near.get(k))) all.add(near.get(k));
        }

        return all;
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

    private static FrameManager load(CompoundTag tag, HolderLookup.Provider registries)
    {
        FrameManager manager = new FrameManager(new FrameGrid(tag.getInt("radius"), Math.max(tag.getInt("spacing"), 1), tag.getInt("y")));
        manager.nextId = Math.max(tag.getInt("next"), 1);
        manager.shiftEpoch = tag.contains("shiftEpoch") ? tag.getLong("shiftEpoch") : manager.shiftEpoch;
        loadCellShifts(manager, tag.getList("cellShifts", Tag.TAG_COMPOUND));

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
            loadRecords(f, t.getList("members", Tag.TAG_COMPOUND));

            manager.frames.add(f);
            manager.byCell[cell] = f;
            manager.nextId = Math.max(manager.nextId, id + 1);
        }

        if (!manager.frames.isEmpty() && !manager.grid.sameAs(Config.FRAME_GRID_RADIUS.get(), Config.FRAME_CELL_SPACING.get(), Config.FRAME_GRID_Y.get()))
            Pioneer.LOGGER.warn("frame grid config changed, keeping saved grid (radius {}, spacing {}, y {}) until all frames are released", manager.grid.radius(), manager.grid.spacing(), manager.grid.y());

        return manager;
    }

    private static void loadCellShifts(FrameManager manager, ListTag list)
    {
        for (int i = 0, n = list.size(); i < n; ++i)
        {
            CompoundTag t = list.getCompound(i);
            int cell = manager.grid.cell(t.getInt("ix"), t.getInt("iz"));
            if (cell < 0) continue;

            manager.cellShift[cell] = readShift(t, new FrameShift());
        }
    }

    private static FrameShift readShift(CompoundTag t, FrameShift out)
    {
        out.identity();
        if (!t.contains("qw")) return out;

        out.qx = t.getDouble("qx");
        out.qy = t.getDouble("qy");
        out.qz = t.getDouble("qz");
        out.qw = t.getDouble("qw");
        out.tx = t.getDouble("tx");
        out.ty = t.getDouble("ty");
        out.tz = t.getDouble("tz");
        out.lx = t.getDouble("lx");
        out.ly = t.getDouble("ly");
        out.lz = t.getDouble("lz");
        out.wx = t.getDouble("wx");
        out.wy = t.getDouble("wy");
        out.wz = t.getDouble("wz");
        out.cx = t.getDouble("cx");
        out.cy = t.getDouble("cy");
        out.cz = t.getDouble("cz");
        return out.markRotation();
    }

    private static CompoundTag writeShift(FrameShift s, CompoundTag t)
    {
        t.putDouble("qx", s.qx);
        t.putDouble("qy", s.qy);
        t.putDouble("qz", s.qz);
        t.putDouble("qw", s.qw);
        t.putDouble("tx", s.tx);
        t.putDouble("ty", s.ty);
        t.putDouble("tz", s.tz);
        t.putDouble("lx", s.lx);
        t.putDouble("ly", s.ly);
        t.putDouble("lz", s.lz);
        t.putDouble("wx", s.wx);
        t.putDouble("wy", s.wy);
        t.putDouble("wz", s.wz);
        t.putDouble("cx", s.cx);
        t.putDouble("cy", s.cy);
        t.putDouble("cz", s.cz);
        return t;
    }

    public void stamp(Entity entity)
    {
        CompoundTag data = entity.getPersistentData();
        int cell = grid.cellAt(entity.getX(), entity.getZ());
        if (cell < 0)
        {
            data.remove(STAMP);
            return;
        }

        CompoundTag t = new CompoundTag();
        t.putLong("epoch", shiftEpoch);
        t.putInt("cell", cell);
        FrameShift shift = cellShift[cell];
        if (shift != null) writeShift(shift, t);

        data.put(STAMP, t);
    }

    public boolean restore(Entity entity)
    {
        int cell = grid.cellAt(entity.getX(), entity.getZ());
        if (cell < 0) return false;

        FrameShift now = cellShift[cell];
        CompoundTag data = entity.getPersistentData();
        if (data.contains(STAMP, Tag.TAG_COMPOUND))
        {
            CompoundTag t = data.getCompound(STAMP);
            if (t.getLong("epoch") != shiftEpoch || t.getInt("cell") != cell) return false;

            readShift(t, stampShift);
        }
        else if (now == null || shiftEpoch != 1L) return false;
        else stampShift.identity();

        if (now == null ? stampShift.isIdentity() : stampShift.sameAs(now)) return false;

        stampShift.invert();
        if (now != null) stampShift.then(now);

        FrameTransfer.settle(entity, stampShift, stampPoint);
        stamp(entity);
        return true;
    }

    private static double halfDiagonal(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        double dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        return 0.5 * Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void loadRecords(LocalFrame f, ListTag list)
    {
        for (int i = 0, n = list.size(); i < n; ++i)
        {
            CompoundTag r = list.getCompound(i);
            if (!r.hasUUID("id")) continue;

            UUID id = r.getUUID("id");
            if (f.record(id) != null) continue;

            LocalFrame.Member m = f.addRecord(id);
            m.mass = r.getDouble("mass");
            m.box.set(r.getDouble("x0"), r.getDouble("y0"), r.getDouble("z0"), r.getDouble("x1"), r.getDouble("y1"), r.getDouble("z1"));
        }

        f.dormant = f.records.size();
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

    public boolean live(LocalFrame frame)
    {
        return frame.cell < byCell.length && byCell[frame.cell] == frame;
    }

    private void requireLive(LocalFrame frame)
    {
        if (!live(frame)) throw new FrameError("Frame #" + frame.id + " no longer exists");
    }

    static boolean due(double ox, double oy, double oz, double ux, double uy, double uz, double distance, double speed, double ahead, double room)
    {
        if (ux * ux + uy * uy + uz * uz > speed * speed) return true;

        double limit = distance * distance;
        if (ox * ox + oy * oy + oz * oz > limit) return true;

        double fx = ox + ux * ahead, fy = oy + uy * ahead, fz = oz + uz * ahead;
        if (fx * fx + fy * fy + fz * fz > limit) return true;

        double height = Math.min(0.5 * room, distance);
        return Math.abs(oy) > height || Math.abs(fy) > height;
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

    private static boolean loadedElsewhere(@Nullable ServerSubLevelContainer container, UUID id)
    {
        if (container == null) return false;

        for (ServerSubLevel subLevel : container.getAllSubLevels())
            if (!subLevel.isRemoved() && id.equals(subLevel.getUniqueId())) return true;

        return false;
    }

    private static ListTag saveRecords(LocalFrame f)
    {
        ListTag list = new ListTag();
        for (int i = 0, n = f.records.size(); i < n; ++i)
        {
            LocalFrame.Member m = f.records.get(i);
            CompoundTag r = new CompoundTag();
            r.putUUID("id", m.id);
            r.putDouble("mass", m.mass);
            r.putDouble("x0", m.box.minX);
            r.putDouble("y0", m.box.minY);
            r.putDouble("z0", m.box.minZ);
            r.putDouble("x1", m.box.maxX);
            r.putDouble("y1", m.box.maxY);
            r.putDouble("z1", m.box.maxZ);
            list.add(r);
        }

        return list;
    }

    private void shiftCell(int cell, FrameShift shift)
    {
        FrameShift total = cellShift[cell];
        if (total == null) cellShift[cell] = total = new FrameShift().identity();

        total.then(shift);
        setDirty();
    }

    private void settleRecords(ServerLevel level, @Nullable ServerSubLevelContainer container)
    {
        long now = level.getGameTime();
        for (int i = 0, n = frames.size(); i < n; ++i)
        {
            LocalFrame f = frames.get(i);
            int dormant = 0;
            for (int r = f.records.size() - 1; r >= 0; --r)
            {
                LocalFrame.Member m = f.records.get(r);
                if (m.loaded) continue;

                String gone = gone(level, container, m, now);
                if (gone == null)
                {
                    ++dormant;
                    continue;
                }

                f.removeRecord(r);
                setDirty();
                Pioneer.LOGGER.info("frame #{} forgets member {}: {}", f.id, m.id.toString().substring(0, 8), gone);
            }

            f.dormant = dormant;
        }
    }

    @Nullable
    private String gone(ServerLevel level, @Nullable ServerSubLevelContainer container, LocalFrame.Member m, long now)
    {
        if (loadedElsewhere(container, m.id)) return "loaded outside its cell";
        if (!areaTicking(level, m.box))
        {
            m.absentSince = -1L;
            return null;
        }

        if (m.absentSince < 0L) m.absentSince = now;

        return now - m.absentSince >= FORGET_TICKS ? "absent from its loaded area" : null;
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
        FrameShift shift = new FrameShift().translation(grid.centerX(cell) - anchor.x, grid.centerY() - anchor.y, grid.centerZ(cell) - anchor.z, -shipVelocity.x, -shipVelocity.y, -shipVelocity.z);
        FrameReport report = FrameTransfer.carry(level, subLevels, players, entities, shift);

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
        requireLive(frame);
        refresh(level);
        if (!frame.members.isEmpty() || !frame.players.isEmpty())
            throw new FrameError("Frame #" + frame.id + " is not empty, release it instead");

        remove(level, frame);
    }

    private boolean areaTicking(ServerLevel level, FrameBox box)
    {
        int x0 = SectionPos.blockToSectionCoord(Mth.floor(box.minX)), x1 = SectionPos.blockToSectionCoord(Mth.floor(box.maxX));
        int z0 = SectionPos.blockToSectionCoord(Mth.floor(box.minZ)), z1 = SectionPos.blockToSectionCoord(Mth.floor(box.maxZ));
        for (int cx = x0; cx <= x1; ++cx)
        {
            for (int cz = z0; cz <= z1; ++cz)
            {
                if (!level.isPositionEntityTicking(probe.set(SectionPos.sectionToBlockCoord(cx), 0, SectionPos.sectionToBlockCoord(cz))))
                    return false;
            }
        }

        return true;
    }

    public void sweep(ServerLevel level)
    {
        if (frames.isEmpty()) return;

        refresh(level);
        sweepEmpty(level);
    }

    public void maintain(ServerLevel level)
    {
        maintain(level, passReport);
    }

    private void setGrid(FrameGrid grid)
    {
        this.grid = grid;
        this.byCell = new LocalFrame[grid.cellCount()];
        this.strays = new int[grid.cellCount()];
        this.cellShift = new FrameShift[grid.cellCount()];
        ++shiftEpoch;
    }

    private void remove(ServerLevel level, LocalFrame frame)
    {
        if (!live(frame)) return;

        int cell = frame.cell;
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
        requireLive(frame);

        ship.updateBoundingBox();
        Vector3d at = new Vector3d(ship.logicalPose().position());
        LocalFrame existing = at(at.x, at.z);
        if (existing != null) throw new FrameError("Ship is already in frame #" + existing.id);

        int cell = frame.cell;
        double cx = grid.centerX(cell), cy = grid.centerY(), cz = grid.centerZ(cell), half = grid.halfCell();
        FrameShift shift = shift(null, frame, level.getGameTime());
        double[] o = shift.position(at.x, at.y, at.z, new double[3]);
        BoundingBox3dc b = ship.boundingBox();
        double[] box = shift.bounds(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), new double[6]);
        if (box[0] - cx < -half || box[3] - cx > half || box[2] - cz < -half || box[5] - cz > half)
            throw new FrameError(String.format(Locale.ROOT, "Ship is %.0f from frame #%d, outside its cell (half %.0f)", Math.sqrt((o[0] - cx) * (o[0] - cx) + (o[1] - cy) * (o[1] - cy) + (o[2] - cz) * (o[2] - cz)), frame.id, half));
        if (box[1] < level.getMinBuildHeight() || box[4] > level.getMaxBuildHeight())
            throw new FrameError("Ship would land outside the height limits of frame #" + frame.id);

        FrameReport report = FrameTransfer.carry(level, List.of(ship), crewOf(level, null, ship), entitiesNear(level, ship), shift);

        refresh(level);
        Pioneer.LOGGER.info("frame {} joined #{}", FrameTransfer.shortId(ship), frame.id);
        NeoForge.EVENT_BUS.post(new FrameEvent.Joined(level, frame, ship));
        return report;
    }

    public FrameReport recenter(ServerLevel level, LocalFrame frame)
    {
        requireLive(frame);
        refresh(level);
        if (!barycenter(frame, SubLevelContainer.getContainer(level)))
            throw new FrameError("Frame #" + frame.id + " has no ship to center on");
        if (frame.dormant > 0)
            throw new FrameError("Frame #" + frame.id + " has " + frame.dormant + " dormant member(s), they cannot be moved");

        int fit = fit(level, frame);
        if (fit == WAIT_CELL) throw new FrameError("Frame #" + frame.id + " content would leave its cell");
        if (fit == WAIT_HEIGHT) throw new FrameError("Frame #" + frame.id + " content would leave the height limits");

        FrameReport report = move(level, frame);
        refresh(level);
        return report;
    }

    public void refresh(ServerLevel level)
    {
        for (int i = 0, n = frames.size(); i < n; ++i) frames.get(i).clearMembers();

        Arrays.fill(strays, 0);
        strayShips.clear();
        strayPlayers.clear();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null)
        {
            for (ServerSubLevel subLevel : container.getAllSubLevels())
            {
                if (subLevel.isRemoved()) continue;

                Vector3d p = subLevel.logicalPose().position();
                int cell = grid.cellAt(p.x, p.z);
                LocalFrame f = cell < 0 ? null : byCell[cell];
                if (f != null)
                {
                    if (f.addMember(subLevel, FrameTransfer.massOf(subLevel))) setDirty();

                    continue;
                }

                strayShips.add(subLevel);
                if (cell >= 0) ++strays[cell];
            }
        }

        List<ServerPlayer> players = level.players();
        for (int i = 0, n = players.size(); i < n; ++i)
        {
            ServerPlayer player = players.get(i);
            int cell = grid.cellAt(player.getX(), player.getZ());
            LocalFrame f = cell < 0 ? null : byCell[cell];
            if (f != null)
            {
                f.addPlayer(player);
                continue;
            }

            if (player.isAlive()) strayPlayers.add(player);
            if (cell >= 0) ++strays[cell];
        }

        settleRecords(level, container);
    }

    public FrameMaintenance maintain(ServerLevel level, FrameMaintenance out)
    {
        out.clear();
        refresh(level);
        if (frames.isEmpty() && strayShips.isEmpty() && strayPlayers.isEmpty()) return out;

        out.adopted = adoptStrays(level, out);
        if (out.adopted > 0) refresh(level);

        out.split = splitFrames(level, out);
        if (out.split > 0) refresh(level);

        out.recentered = recenterDrifting(level, out);
        if (out.recentered > 0) refresh(level);

        out.swept = sweepEmpty(level);
        out.frames = frames.size();
        for (int i = 0, n = frames.size(); i < n; ++i) out.dormant += frames.get(i).dormant;

        return out;
    }

    private int recenterDrifting(ServerLevel level, FrameMaintenance out)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return 0;

        double distance = Math.min(Config.FRAME_RECENTER_DISTANCE.get(), Math.max(16.0, grid.halfCell() - Config.FRAME_EDGE_MARGIN.get()));
        double speed = Config.FRAME_RECENTER_SPEED.get();
        int period = Config.FRAME_MAINTAIN_PERIOD.get();
        double ahead = period > 0 ? period / TICKS_PER_SECOND : 1.0;
        double cy = grid.centerY(), room = Math.min(level.getMaxBuildHeight() - cy, cy - level.getMinBuildHeight());
        int moved = 0;
        for (int i = 0; i < frames.size(); ++i)
        {
            LocalFrame f = frames.get(i);
            boolean edge = f.edgeRecenter;
            f.edgeRecenter = false;
            if (!barycenter(f, container) || !(edge || due(centerX, centerY, centerZ, driftX, driftY, driftZ, distance, speed, ahead, room)))
            {
                f.waitKey = WAIT_NONE;
                continue;
            }

            int wait = f.dormant > 0 ? WAIT_DORMANT + 4 * f.dormant : fit(level, f);
            if (wait != WAIT_NONE)
            {
                waiting(f, wait, out);
                continue;
            }

            f.waitKey = WAIT_NONE;
            double offset = Math.sqrt(centerX * centerX + centerY * centerY + centerZ * centerZ), drift = Math.sqrt(driftX * driftX + driftY * driftY + driftZ * driftZ);
            move(level, f);
            Pioneer.LOGGER.info("frame #{} recentered by the pass: offset {} speed {} u/s", f.id, String.format(Locale.ROOT, "%.1f", offset), String.format(Locale.ROOT, "%.1f", drift));
            ++moved;
        }

        return moved;
    }

    public void adopt(ServerLevel level)
    {
        refresh(level);
        if (strayShips.isEmpty() && strayPlayers.isEmpty()) return;

        if (adoptStrays(level, passReport.clear()) > 0) refresh(level);
    }

    private int adoptStrays(ServerLevel level, FrameMaintenance out)
    {
        int ships = strayShips.size(), people = strayPlayers.size();
        if (ships + people == 0)
        {
            adoptWaitKey = ADOPT_NONE;
            return 0;
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        PhysicsPipeline pipeline = container == null ? null : container.physicsSystem().getPipeline();
        groups.clear();
        for (int i = 0; i < ships; ++i) addShip(strayShips.get(i), pipeline);

        for (int i = 0; i < people; ++i) addPlayer(strayPlayers.get(i));

        groups.link(Config.FRAME_CLUSTER_LINK.get());
        int adopted = 0;
        long now = level.getGameTime();
        for (int g = 0; g < groups.groups; ++g)
        {
            List<ServerSubLevel> groupShips = new ArrayList<>(groups.groupShips[g]);
            List<ServerPlayer> groupPlayers = new ArrayList<>(groups.groupPlayers[g]);
            gather(g, strayShips, strayPlayers, groupShips, groupPlayers);
            LocalFrame near = nearFrame(g, now);
            if (near != null && joinGroup(level, near, groupShips, groupPlayers, now))
            {
                ++adopted;
                continue;
            }

            if (openGroup(level, g, groupShips, groupPlayers, now, out)) ++adopted;
        }

        return adopted;
    }

    private void addShip(ServerSubLevel ship, @Nullable PhysicsPipeline pipeline)
    {
        BoundingBox3dc b = ship.boundingBox();
        Vector3d p = ship.logicalPose().position();
        if (pipeline == null) linear.zero();
        else pipeline.getLinearVelocity(ship, linear);

        groups.add(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), FrameTransfer.massOf(ship), p.x, p.y, p.z, linear.x, linear.y, linear.z, true);
    }

    private void addPlayer(ServerPlayer player)
    {
        AABB b = player.getBoundingBox();
        groups.add(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, 0.0, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0, false);
    }

    private void gather(int g, List<ServerSubLevel> ships, List<ServerPlayer> players, List<ServerSubLevel> shipsOut, List<ServerPlayer> playersOut)
    {
        for (int i = 0; i < groups.count; ++i)
        {
            if (groups.group[i] != g) continue;

            if (i < groups.ships) shipsOut.add(ships.get(i));
            else playersOut.add(players.get(i - groups.ships));
        }
    }

    @Nullable
    private LocalFrame nearFrame(int g, long now)
    {
        double link = Config.FRAME_CLUSTER_LINK.get();
        int b = g * 6;
        double gx = 0.5 * (groups.groupBox[b] + groups.groupBox[b + 3]), gy = 0.5 * (groups.groupBox[b + 1] + groups.groupBox[b + 4]), gz = 0.5 * (groups.groupBox[b + 2] + groups.groupBox[b + 5]);
        double gr = halfDiagonal(groups.groupBox[b], groups.groupBox[b + 1], groups.groupBox[b + 2], groups.groupBox[b + 3], groups.groupBox[b + 4], groups.groupBox[b + 5]);
        double[] c = new double[3];
        LocalFrame best = null;
        double bestGap = link;
        for (int i = 0, n = frames.size(); i < n; ++i)
        {
            LocalFrame f = frames.get(i);
            FrameBox box = f.box;
            if (box.isEmpty()) continue;

            virtualOf(f, 0.5 * (box.minX + box.maxX), 0.5 * (box.minY + box.maxY), 0.5 * (box.minZ + box.maxZ), now, 0.0, c);
            double dx = c[0] - gx, dy = c[1] - gy, dz = c[2] - gz;
            double gap = Math.sqrt(dx * dx + dy * dy + dz * dz) - gr - halfDiagonal(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            if (gap > bestGap) continue;

            bestGap = gap;
            best = f;
        }

        return best;
    }

    private boolean joinGroup(ServerLevel level, LocalFrame frame, List<ServerSubLevel> ships, List<ServerPlayer> players, long now)
    {
        FrameShift shift = shift(null, frame, now);
        if (!fits(level, frame.cell, groupBounds(ships, players, shift, new double[6]))) return false;

        FrameTransfer.carry(level, ships, players, entitiesNear(level, ships), shift);
        Pioneer.LOGGER.info("frame #{} adopts ships={} players={}", frame.id, ships.size(), players.size());
        for (int i = 0, n = ships.size(); i < n; ++i)
            NeoForge.EVENT_BUS.post(new FrameEvent.Joined(level, frame, ships.get(i)));

        return true;
    }

    private double[] groupBounds(List<ServerSubLevel> ships, List<ServerPlayer> players, FrameShift shift, double[] out)
    {
        FrameBox all = new FrameBox();
        double[] one = new double[6];
        for (int i = 0, n = ships.size(); i < n; ++i)
        {
            BoundingBox3dc b = ships.get(i).boundingBox();
            shift.bounds(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), one);
            all.include(one[0], one[1], one[2], one[3], one[4], one[5]);
        }

        for (int i = 0, n = players.size(); i < n; ++i)
        {
            AABB b = players.get(i).getBoundingBox();
            shift.bounds(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, one);
            all.include(one[0], one[1], one[2], one[3], one[4], one[5]);
        }

        out[0] = all.minX;
        out[1] = all.minY;
        out[2] = all.minZ;
        out[3] = all.maxX;
        out[4] = all.maxY;
        out[5] = all.maxZ;
        return out;
    }

    private boolean fits(ServerLevel level, int cell, double[] box)
    {
        double cx = grid.centerX(cell), cz = grid.centerZ(cell), half = grid.halfCell();
        if (box[0] < cx - half || box[3] > cx + half || box[2] < cz - half || box[5] > cz + half) return false;

        return box[1] >= level.getMinBuildHeight() && box[4] <= level.getMaxBuildHeight();
    }

    private boolean openGroup(ServerLevel level, int g, List<ServerSubLevel> ships, List<ServerPlayer> players, long now, FrameMaintenance out)
    {
        int cell = grid.nearestFree(c -> byCell[c] == null && strays[c] == itemsIn(g, c));
        if (cell < 0)
        {
            adoptWaiting(out);
            return false;
        }

        adoptWaitKey = ADOPT_NONE;
        int p = g * 3;
        double x = groups.groupPos[p], y = groups.groupPos[p + 1], z = groups.groupPos[p + 2];
        double ux = groups.groupVel[p], uy = groups.groupVel[p + 1], uz = groups.groupVel[p + 2];
        LocalFrame frame = new LocalFrame(nextId++, cell);
        frame.motion.set(now, x, y, z, ux / TICKS_PER_SECOND, uy / TICKS_PER_SECOND, uz / TICKS_PER_SECOND, 0.0, 0.0, 0.0);
        frame.auto = true;
        frames.add(frame);
        byCell[cell] = frame;
        setDirty();
        FrameSync.upsert(level, this, frame);
        FrameShift shift = new FrameShift().translation(grid.centerX(cell) - x, grid.centerY() - y, grid.centerZ(cell) - z, -ux, -uy, -uz);
        FrameTransfer.carry(level, ships, players, entitiesNear(level, ships), shift);
        Pioneer.LOGGER.info("frame adopted #{} at ({},{}): ships={} players={}", frame.id, grid.ix(cell), grid.iz(cell), ships.size(), players.size());
        NeoForge.EVENT_BUS.post(new FrameEvent.Created(level, frame));
        return true;
    }

    private int itemsIn(int g, int cell)
    {
        int n = 0;
        for (int i = 0; i < groups.count; ++i)
        {
            if (groups.group[i] == g && grid.cellAt(groups.pos[i * 3], groups.pos[i * 3 + 2]) == cell) ++n;
        }

        return n;
    }

    private void adoptWaiting(FrameMaintenance out)
    {
        int key = 1 + frames.size();
        if (adoptWaitKey != key)
        {
            adoptWaitKey = key;
            adoptWaitText = "adopt waiting: grid full " + frames.size() + "/" + grid.cellCount();
            Pioneer.LOGGER.warn("frame {}", adoptWaitText);
        }

        out.waiting.add(adoptWaitText);
    }

    private int splitFrames(ServerLevel level, FrameMaintenance out)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        PhysicsPipeline pipeline = container == null ? null : container.physicsSystem().getPipeline();
        double link = Config.FRAME_CLUSTER_LINK.get(), apart = Config.FRAME_SPLIT_DISTANCE.get(), margin = Config.FRAME_EDGE_MARGIN.get();
        double distance = Math.min(Config.FRAME_RECENTER_DISTANCE.get(), Math.max(16.0, grid.halfCell() - margin)), speed = Config.FRAME_RECENTER_SPEED.get();
        int period = Config.FRAME_MAINTAIN_PERIOD.get();
        double ahead = period > 0 ? period / TICKS_PER_SECOND : 1.0;
        double cy = grid.centerY(), room = Math.min(level.getMaxBuildHeight() - cy, cy - level.getMinBuildHeight());
        long now = level.getGameTime();
        int split = 0;
        for (int i = 0, n = frames.size(); i < n; ++i)
        {
            LocalFrame f = frames.get(i);
            f.edgeRecenter = false;
            int ships = f.members.size(), people = f.players.size();
            if (ships + people == 0) continue;

            groups.clear();
            for (int s = 0; s < ships; ++s) addShip(f.members.get(s), pipeline);

            for (int s = 0; s < people; ++s) addPlayer(f.players.get(s));

            groups.link(link);
            int main = mainGroup(f);
            if (crosses(level, f, main, 2.0 * ahead, margin)) f.edgeRecenter = true;
            if (groups.groups == 1 && f.dormant == 0) continue;

            for (int g = 0; g < groups.groups; ++g)
            {
                FrameEvent.Reason reason = reason(level, f, g, main, apart, margin, 2.0 * ahead, distance, speed, ahead, room);
                if (reason == null) continue;

                List<ServerSubLevel> groupShips = new ArrayList<>(groups.groupShips[g]);
                List<ServerPlayer> groupPlayers = new ArrayList<>(groups.groupPlayers[g]);
                gather(g, f.members, f.players, groupShips, groupPlayers);
                if (splitGroup(level, f, g, groupShips, groupPlayers, reason, now, out)) ++split;
            }
        }

        return split;
    }

    private int mainGroup(LocalFrame f)
    {
        int anchor = f.anchor == null ? -1 : groups.group[f.members.indexOf(f.anchor)];
        int best = 0;
        for (int g = 1; g < groups.groups; ++g)
        {
            double dm = groups.groupMass[g] - groups.groupMass[best];
            if (dm > 0.0)
            {
                best = g;
                continue;
            }

            if (dm < 0.0 || best == anchor) continue;
            if (g == anchor || groups.groupShips[g] > groups.groupShips[best] || (groups.groupShips[g] == groups.groupShips[best] && groups.groupPlayers[g] > groups.groupPlayers[best]))
                best = g;
        }

        return best;
    }

    @Nullable
    private FrameEvent.Reason reason(ServerLevel level, LocalFrame f, int g, int main, double apart, double margin, double horizon, double distance, double speed, double ahead, double room)
    {
        if (g != main && groups.groupGap(g, main) > apart) return FrameEvent.Reason.DISTANCE;
        if (g != main && crosses(level, f, g, horizon, margin)) return FrameEvent.Reason.EDGE;
        if (f.dormant == 0) return null;

        int p = g * 3;
        double ox = groups.groupPos[p] - grid.centerX(f.cell), oy = groups.groupPos[p + 1] - grid.centerY(), oz = groups.groupPos[p + 2] - grid.centerZ(f.cell);
        return due(ox, oy, oz, groups.groupVel[p], groups.groupVel[p + 1], groups.groupVel[p + 2], distance, speed, ahead, room) ? FrameEvent.Reason.DORMANT : null;
    }

    private boolean crosses(ServerLevel level, LocalFrame f, int g, double horizon, double margin)
    {
        int b = g * 6, p = g * 3;
        double[] box = groups.groupBox;
        double vx = groups.groupVel[p] * horizon, vy = groups.groupVel[p + 1] * horizon, vz = groups.groupVel[p + 2] * horizon;
        double cx = grid.centerX(f.cell), cz = grid.centerZ(f.cell), half = grid.halfCell() - margin;
        if (box[b] + Math.min(0.0, vx) < cx - half || box[b + 3] + Math.max(0.0, vx) > cx + half) return true;
        if (box[b + 2] + Math.min(0.0, vz) < cz - half || box[b + 5] + Math.max(0.0, vz) > cz + half) return true;

        return box[b + 1] + Math.min(0.0, vy) < level.getMinBuildHeight() || box[b + 4] + Math.max(0.0, vy) > level.getMaxBuildHeight();
    }

    private boolean splitGroup(ServerLevel level, LocalFrame f, int g, List<ServerSubLevel> ships, List<ServerPlayer> players, FrameEvent.Reason reason, long now, FrameMaintenance out)
    {
        int cell = grid.nearestFree(c -> byCell[c] == null && strays[c] == 0);
        if (cell < 0)
        {
            splitWaiting(f, out);
            return false;
        }

        f.splitWaitKey = 0;
        int p = g * 3;
        double ox = groups.groupPos[p] - grid.centerX(f.cell), oy = groups.groupPos[p + 1] - grid.centerY(), oz = groups.groupPos[p + 2] - grid.centerZ(f.cell);
        double ux = groups.groupVel[p], uy = groups.groupVel[p + 1], uz = groups.groupVel[p + 2];
        LocalFrame into = new LocalFrame(nextId++, cell);
        into.motion.set(f.motion);
        into.motion.recenter(now, ox, oy, oz, ux / TICKS_PER_SECOND, uy / TICKS_PER_SECOND, uz / TICKS_PER_SECOND);
        into.attach(f.parentName, f.fixed);
        into.auto = true;
        for (int i = 0, n = ships.size(); i < n; ++i) f.forget(ships.get(i).getUniqueId());

        frames.add(into);
        byCell[cell] = into;
        setDirty();
        FrameSync.upsert(level, this, into);
        double dx = grid.centerX(cell) - grid.centerX(f.cell), dz = grid.centerZ(cell) - grid.centerZ(f.cell);
        FrameShift shift = new FrameShift().translation(dx - ox, -oy, dz - oz, -ux, -uy, -uz);
        FrameTransfer.carry(level, ships, players, entitiesNear(level, ships), shift);
        String why = reason.name().toLowerCase(Locale.ROOT);
        Pioneer.LOGGER.info("frame #{} split into #{} at ({},{}) ({}): ships={} players={}", f.id, into.id, grid.ix(cell), grid.iz(cell), why, ships.size(), players.size());
        NeoForge.EVENT_BUS.post(new FrameEvent.Created(level, into));
        NeoForge.EVENT_BUS.post(new FrameEvent.Split(level, f, into, List.copyOf(ships), players.size(), reason));
        return true;
    }

    private void splitWaiting(LocalFrame f, FrameMaintenance out)
    {
        int key = 1 + frames.size();
        if (f.splitWaitKey != key)
        {
            f.splitWaitKey = key;
            f.splitWaitText = "split #" + f.id + " waiting: grid full " + frames.size() + "/" + grid.cellCount();
            Pioneer.LOGGER.warn("frame {}", f.splitWaitText);
        }

        out.waiting.add(f.splitWaitText);
    }

    private boolean barycenter(LocalFrame f, @Nullable ServerSubLevelContainer container)
    {
        List<ServerSubLevel> ships = f.members;
        int n = ships.size();
        if (n == 0 || container == null) return false;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        Vector3d first = ships.get(0).logicalPose().position();
        double rx = first.x, ry = first.y, rz = first.z;
        if (n == 1)
        {
            pipeline.getLinearVelocity(ships.get(0), linear);
            return centered(f, rx, ry, rz, linear.x, linear.y, linear.z);
        }

        boolean weightless = f.mass <= 0.0;
        double total = 0.0, sx = 0.0, sy = 0.0, sz = 0.0, px = 0.0, py = 0.0, pz = 0.0;
        for (int i = 0; i < n; ++i)
        {
            ServerSubLevel ship = ships.get(i);
            double m = weightless ? 1.0 : FrameTransfer.massOf(ship);
            Vector3d p = ship.logicalPose().position();
            sx += m * (p.x - rx);
            sy += m * (p.y - ry);
            sz += m * (p.z - rz);
            pipeline.getLinearVelocity(ship, linear);
            px += m * linear.x;
            py += m * linear.y;
            pz += m * linear.z;
            total += m;
        }

        return centered(f, rx + sx / total, ry + sy / total, rz + sz / total, px / total, py / total, pz / total);
    }

    private boolean centered(LocalFrame f, double x, double y, double z, double ux, double uy, double uz)
    {
        centerX = x - grid.centerX(f.cell);
        centerY = y - grid.centerY();
        centerZ = z - grid.centerZ(f.cell);
        driftX = ux;
        driftY = uy;
        driftZ = uz;
        return true;
    }

    private int fit(ServerLevel level, LocalFrame f)
    {
        double cx = grid.centerX(f.cell), cz = grid.centerZ(f.cell), half = grid.halfCell();
        FrameBox box = f.box;
        if (box.minX - centerX < cx - half || box.maxX - centerX > cx + half || box.minZ - centerZ < cz - half || box.maxZ - centerZ > cz + half)
            return WAIT_CELL;
        if (box.minY - centerY < level.getMinBuildHeight() || box.maxY - centerY > level.getMaxBuildHeight())
            return WAIT_HEIGHT;

        return WAIT_NONE;
    }

    private void waiting(LocalFrame f, int key, FrameMaintenance out)
    {
        if (f.waitKey != key)
        {
            f.waitKey = key;
            String reason = key == WAIT_CELL ? "content would leave its cell" : key == WAIT_HEIGHT ? "content would leave the height limits" : f.dormant + " dormant member(s)";
            f.waitText = "recenter #" + f.id + " waiting: " + reason;
            Pioneer.LOGGER.info("frame {}", f.waitText);
        }

        out.waiting.add(f.waitText);
    }

    private FrameReport move(ServerLevel level, LocalFrame f)
    {
        double ox = centerX, oy = centerY, oz = centerZ, ux = driftX, uy = driftY, uz = driftZ;
        f.motion.recenter(level.getGameTime(), ox, oy, oz, ux / TICKS_PER_SECOND, uy / TICKS_PER_SECOND, uz / TICKS_PER_SECOND);
        changed(f, FrameEvent.Cause.RECENTER);
        FrameShift shift = new FrameShift().translation(-ox, -oy, -oz, -ux, -uy, -uz);
        FrameReport report = FrameTransfer.carry(level, List.copyOf(f.members), List.copyOf(f.players), entitiesInCell(level, f.cell), shift);
        shiftCell(f.cell, shift);
        return report;
    }

    public void push(LocalFrame frame, long now, double dvx, double dvy, double dvz)
    {
        requireLive(frame);
        frame.motion.rebase(now).addVelocity(dvx / 20.0, dvy / 20.0, dvz / 20.0);
        changed(frame, FrameEvent.Cause.MOTION);
    }

    public void accelerate(LocalFrame frame, long now, double ax, double ay, double az)
    {
        requireLive(frame);
        frame.motion.rebase(now).accelerate(ax / 400.0, ay / 400.0, az / 400.0);
        changed(frame, FrameEvent.Cause.MOTION);
    }

    public void stop(LocalFrame frame, long now)
    {
        requireLive(frame);
        FrameMotion m = frame.motion.rebase(now);
        m.set(now, m.px, m.py, m.pz, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        changed(frame, FrameEvent.Cause.MOTION);
    }

    public void spin(LocalFrame frame, long now, double wx, double wy, double wz)
    {
        requireLive(frame);
        frame.motion.rebase(now).spin(wx, wy, wz);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void unspin(LocalFrame frame, long now)
    {
        requireLive(frame);
        frame.motion.rebase(now).spin(0.0, 0.0, 0.0).orient(0.0, 0.0, 0.0, 1.0);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void reparent(LocalFrame frame, long now, @Nullable FrameParent to, boolean fixed)
    {
        requireLive(frame);
        pose.reparent(frame.motion, frame.parent(system()), frame.fixed, to, fixed, now);
        frame.attach(to == null ? "" : to.name(), fixed);
        changed(frame, FrameEvent.Cause.PARENT);
    }

    public void orient(LocalFrame frame, long now, double qx, double qy, double qz, double qw)
    {
        requireLive(frame);
        double[] q = FrameQuat.normalize(new double[]{qx, qy, qz, qw});
        frame.motion.rebase(now).orient(q[0], q[1], q[2], q[3]);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void assign(LocalFrame frame, FrameMotion motion, @Nullable FrameParent parent, boolean fixed)
    {
        requireLive(frame);
        double[] q = FrameQuat.normalize(new double[]{motion.qx, motion.qy, motion.qz, motion.qw});
        frame.motion.set(motion).orient(q[0], q[1], q[2], q[3]);
        frame.attach(parent == null ? "" : parent.name(), fixed);
        changed(frame, FrameEvent.Cause.STATE);
    }

    public void place(LocalFrame frame, long now, double x, double y, double z)
    {
        requireLive(frame);
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
        requireLive(frame);
        refresh(level);

        long now = level.getGameTime();
        double[] v = velocityOf(frame, now, new double[3]);
        double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]) * 20.0;
        double limit = Config.FRAME_RELEASE_MAX_SPEED.get();
        if (!force && speed > limit)
            throw new FrameError(String.format(Locale.ROOT, "Frame #%d is too fast (%.1f u/s, max %.1f)", frame.id, speed, limit));

        int cell = frame.cell;
        FrameShift shift = shift(frame, null, now);

        FrameBox box = frame.box;
        if (!box.isEmpty())
        {
            double[] land = shift.bounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, new double[6]);
            double minY = land[1], maxY = land[4];
            if (minY < level.getMinBuildHeight() || maxY > level.getMaxBuildHeight())
                throw new FrameError("Frame #" + frame.id + " would land outside the height limits");

            double minX = land[0], maxX = land[3], minZ = land[2], maxZ = land[5];
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
        FrameReport report = FrameTransfer.carry(level, subLevels, players, entities, shift);
        shiftCell(cell, shift);

        frames.remove(frame);
        byCell[cell] = null;
        frame.clearMembers();
        setDirty();
        FrameSync.remove(level, this, cell);
        Pioneer.LOGGER.info("frame released #{} from ({},{})", frame.id, grid.ix(cell), grid.iz(cell));
        NeoForge.EVENT_BUS.post(new FrameEvent.Released(level, frame));
        return new FrameResult(frame, report);
    }

    private FrameShift shift(@Nullable LocalFrame from, @Nullable LocalFrame to, long now)
    {
        double[] fromVelocity = new double[3], fromSpin = new double[3], toVelocity = new double[3], toSpin = new double[3];
        FramePose a = state(from, now, new FramePose(), fromVelocity, fromSpin);
        FramePose b = state(to, now, new FramePose(), toVelocity, toSpin);
        double fx = from == null ? 0.0 : grid.centerX(from.cell), fy = from == null ? 0.0 : grid.centerY(), fz = from == null ? 0.0 : grid.centerZ(from.cell);
        double tx = to == null ? 0.0 : grid.centerX(to.cell), ty = to == null ? 0.0 : grid.centerY(), tz = to == null ? 0.0 : grid.centerZ(to.cell);
        return new FrameShift().between(a, fx, fy, fz, fromVelocity, fromSpin, b, tx, ty, tz, toVelocity, toSpin).scaleKick(TICKS_PER_SECOND);
    }

    private FramePose state(@Nullable LocalFrame frame, long now, FramePose out, double[] velocity, double[] spin)
    {
        if (frame == null) return out.identity();

        FrameParent parent = frame.parent(system());
        out.velocity(frame.motion, parent, frame.fixed, now, 0.0, velocity);
        out.angularVelocity(frame.motion, parent, frame.fixed, now, spin);
        return out.set(frame.motion, parent, frame.fixed, now, 0.0);
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

    private int sweepEmpty(ServerLevel level)
    {
        int swept = 0;
        for (int i = frames.size() - 1; i >= 0; --i)
        {
            LocalFrame f = frames.get(i);
            if (!f.auto || f.absent > 0 || f.dormant > 0 || !f.members.isEmpty() || !f.players.isEmpty()) continue;

            remove(level, f);
            ++swept;
        }

        return swept;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putInt("radius", grid.radius());
        tag.putInt("spacing", grid.spacing());
        tag.putInt("y", grid.y());
        tag.putInt("next", nextId);
        tag.putLong("shiftEpoch", shiftEpoch);
        ListTag shifts = new ListTag();
        for (int c = 0; c < cellShift.length; ++c)
        {
            if (cellShift[c] == null) continue;

            CompoundTag t = writeShift(cellShift[c], new CompoundTag());
            t.putInt("ix", grid.ix(c));
            t.putInt("iz", grid.iz(c));
            shifts.add(t);
        }

        if (!shifts.isEmpty()) tag.put("cellShifts", shifts);

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

            if (!f.records.isEmpty()) t.put("members", saveRecords(f));

            list.add(t);
        }
        tag.put("frames", list);
        return tag;
    }
}
