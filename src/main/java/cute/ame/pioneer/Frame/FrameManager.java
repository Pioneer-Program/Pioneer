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
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaterniond;
import org.joml.Vector2i;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    private static final int MERGE_DORMANT = 1, MERGE_ABSENT = 2, MERGE_CELL = 3, MERGE_EDGE = 4, MERGE_APART = 5;
    private static final int SPHERE = 8, PRELOAD_TICKS = 300, PRELOAD_REACH = 32;
    private static final int FOLLOW_ALL = 0, FOLLOW_SHIPS = 1, FOLLOW_KEEP = 2;
    private static final TicketType<ChunkPos> PRELOAD = TicketType.create("pioneer_frame_preload", Comparator.comparingLong(ChunkPos::toLong), PRELOAD_TICKS);

    private static final SavedData.Factory<FrameManager> FACTORY = new SavedData.Factory<>(FrameManager::fromConfig, FrameManager::load, null);
    private final ArrayList<LocalFrame> frames = new ArrayList<>(8);
    private final List<LocalFrame> framesView = Collections.unmodifiableList(frames);
    private final FramePose pose = new FramePose();
    private final FrameMaintenance passReport = new FrameMaintenance();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final Vector3d linear = new Vector3d();
    private final FrameShift stampShift = new FrameShift();
    private final Vector3d stampPoint = new Vector3d();
    private final FrameGroups groups = new FrameGroups();
    private final ArrayList<ServerSubLevel> strayShips = new ArrayList<>(4);
    private final ArrayList<ServerPlayer> strayPlayers = new ArrayList<>(2);
    private final ArrayList<CellForward> forwards = new ArrayList<>(4);
    private final FramePose mergePose = new FramePose();
    private final FrameBox content = new FrameBox();
    private final FrameShift scratchShift = new FrameShift(), mergeShift = new FrameShift();
    private final FramePose shiftFrom = new FramePose(), shiftTo = new FramePose();
    private final Vector3d fromVelocity = new Vector3d(), fromSpin = new Vector3d(), toVelocity = new Vector3d(), toSpin = new Vector3d();
    private final double[] mergeBox = new double[6];
    private final Vector3d mergePoint = new Vector3d(), mergeVelocity = new Vector3d(), mergeSpin = new Vector3d();
    private double[] spheres = new double[8 * SPHERE];
    private ServerSubLevel[] carried = new ServerSubLevel[2];
    private LocalFrame[] pairFrom = new LocalFrame[4], pairTo = new LocalFrame[4];
    private double[] pairGap = new double[4];
    private boolean[] pairPredicted = new boolean[4];
    private int pairs;
    private int adoptWaitKey;
    private String adoptWaitText = "";
    private double centerX, centerY, centerZ, driftX, driftY, driftZ;
    private FrameShift[] cellShift;
    private long[] cellLine;
    private long shiftEpoch, lineCount;
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
        loadCellLines(manager, tag.getList("cellLines", Tag.TAG_COMPOUND), tag.getList("cellForwards", Tag.TAG_COMPOUND));
        manager.lineCount = tag.getLong("lineCount");

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
                Quaterniond q = FrameQuat.normalize(new Quaterniond(t.getDouble("qx"), t.getDouble("qy"), t.getDouble("qz"), t.getDouble("qw")));
                f.motion.orient(q.x, q.y, q.z, q.w).spin(t.getDouble("wx"), t.getDouble("wy"), t.getDouble("wz"));
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

    private static void loadCellLines(FrameManager manager, ListTag lines, ListTag links)
    {
        for (int i = 0, n = lines.size(); i < n; ++i)
        {
            CompoundTag t = lines.getCompound(i);
            int cell = manager.grid.cell(t.getInt("ix"), t.getInt("iz"));
            if (cell >= 0) manager.cellLine[cell] = t.getLong("line");
        }

        for (int i = 0, n = links.size(); i < n; ++i)
        {
            CompoundTag t = links.getCompound(i);
            int from = manager.grid.cell(t.getInt("ix"), t.getInt("iz")), to = manager.grid.cell(t.getInt("tix"), t.getInt("tiz"));
            if (from < 0 || to < 0) continue;

            manager.forwards.add(new CellForward(from, t.getLong("line"), to, t.getLong("toLine"), readShift(t, new FrameShift())));
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

    static double closest(double rx, double ry, double rz, double ux, double uy, double uz, double horizon)
    {
        double uu = ux * ux + uy * uy + uz * uz;
        double t = uu > 0.0 ? Math.max(0.0, Math.min(horizon, -(rx * ux + ry * uy + rz * uz) / uu)) : 0.0;
        double dx = rx + ux * t, dy = ry + uy * t, dz = rz + uz * t;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void ticket(ServerLevel level, LocalFrame.Member m)
    {
        FrameBox box = m.box;
        int x0 = SectionPos.blockToSectionCoord(Mth.floor(box.minX)), x1 = SectionPos.blockToSectionCoord(Mth.floor(box.maxX));
        int z0 = SectionPos.blockToSectionCoord(Mth.floor(box.minZ)), z1 = SectionPos.blockToSectionCoord(Mth.floor(box.maxZ));
        int cx = (x0 + x1) >> 1, cz = (z0 + z1) >> 1;
        int reach = Math.min(PRELOAD_REACH, 2 + Math.max(x1 - x0 + 1, z1 - z0 + 1) / 2);
        ChunkPos pos = m.ticket;
        if (pos == null || pos.x != cx || pos.z != cz) m.ticket = pos = new ChunkPos(cx, cz);

        level.getChunkSource().addRegionTicket(PRELOAD, pos, reach, pos);
    }

    private static LocalFrame heavier(LocalFrame a, LocalFrame b)
    {
        if (a.mass != b.mass) return a.mass > b.mass ? a : b;
        if (a.players.size() != b.players.size()) return a.players.size() > b.players.size() ? a : b;

        return a.id < b.id ? a : b;
    }

    private static LocalFrame keeper(LocalFrame a, LocalFrame b)
    {
        if ((a.absent > 0) != (b.absent > 0)) return a.absent > 0 ? a : b;
        if (a.players.isEmpty() != b.players.isEmpty()) return a.players.isEmpty() ? b : a;

        return heavier(a, b);
    }

    private static String mergeReason(LocalFrame from, LocalFrame kept, int reason)
    {
        return switch (reason)
        {
            case MERGE_DORMANT -> (kept.dormant + from.dormant) + " dormant member(s)";
            case MERGE_ABSENT -> "logged-out player(s) in both frames";
            case MERGE_CELL -> "content would leave the cell of #" + kept.id;
            case MERGE_EDGE -> "content would reach the edge of the cell of #" + kept.id;
            default -> "content farther than the split distance";
        };
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

    private void shiftCell(int cell, FrameShift shift)
    {
        FrameShift total = cellShift[cell];
        if (total == null) cellShift[cell] = total = new FrameShift().identity();

        total.then(shift);
        setDirty();
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
        if (cellLine[cell] != 0L) t.putLong("line", cellLine[cell]);

        FrameShift shift = cellShift[cell];
        if (shift != null) writeShift(shift, t);

        data.put(STAMP, t);
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

    public Vector3d originOf(LocalFrame f, long tick, double partial, Vector3d out)
    {
        return poseOf(f, tick, partial, pose).toSystem(0.0, 0.0, 0.0, out);
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

    public Vector3d velocityOf(LocalFrame f, long tick, Vector3d out)
    {
        return pose.velocity(f.motion, f.parent(system()), f.fixed, tick, 0.0, out);
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

    public boolean restore(Entity entity)
    {
        int cell = grid.cellAt(entity.getX(), entity.getZ());
        if (cell < 0) return false;

        long line = 0L;
        CompoundTag data = entity.getPersistentData();
        if (data.contains(STAMP, Tag.TAG_COMPOUND))
        {
            CompoundTag t = data.getCompound(STAMP);
            if (t.getLong("epoch") != shiftEpoch || t.getInt("cell") != cell) return false;

            line = t.getLong("line");
            readShift(t, stampShift);
        }
        else if (shiftEpoch != 1L) return false;
        else stampShift.identity();

        if (line != cellLine[cell]) cell = follow(cell, line);
        else if (cellShift[cell] == null ? stampShift.isIdentity() : stampShift.sameAs(cellShift[cell])) return false;
        else stampShift.invert();

        if (cell < 0) return false;
        if (cellShift[cell] != null) stampShift.then(cellShift[cell]);

        FrameTransfer.settle(entity, stampShift, stampPoint);
        stamp(entity);
        return true;
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

    public Vector3d virtualOf(LocalFrame f, double x, double y, double z, long tick, double partial, Vector3d out)
    {
        return poseOf(f, tick, partial, pose).toSystem(x - grid.centerX(f.cell), y - grid.centerY(), z - grid.centerZ(f.cell), out);
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
            Vector3d p = body.parkingPoint(anchor.x, anchor.y, anchor.z, now, new Vector3d());
            Vector3d b = body.positionAt(now, 0.0, new Vector3d());
            frame.motion.set(now, p.x - b.x, p.y - b.y, p.z - b.z, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
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

    private static boolean overlapsPlotGrid(SubLevelContainer container, double minX, double minZ, double maxX, double maxZ)
    {
        int shift = container.getLogPlotSize() + SectionPos.SECTION_BITS, side = 1 << container.getLogSideLength();
        Vector2i origin = container.getOrigin();
        return (Mth.floor(maxX) >> shift) >= origin.x && (Mth.floor(minX) >> shift) < origin.x + side
            && (Mth.floor(maxZ) >> shift) >= origin.y && (Mth.floor(minZ) >> shift) < origin.y + side;
    }

    private int follow(int cell, long line)
    {
        stampShift.invert();
        for (int hop = 0, n = forwards.size(); hop < n; ++hop)
        {
            CellForward link = forward(cell, line);
            if (link == null) return -1;

            stampShift.then(link.shift());
            cell = link.to();
            line = link.toLine();
            if (line == cellLine[cell]) return cell;
        }

        return -1;
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

    @Nullable
    private CellForward forward(int cell, long line)
    {
        for (int i = 0, n = forwards.size(); i < n; ++i)
        {
            CellForward link = forwards.get(i);
            if (link.from() == cell && link.line() == line) return link;
        }

        return null;
    }

    private void handOver(int from, int to, FrameShift shift)
    {
        FrameShift link = new FrameShift();
        if (cellShift[from] != null) link.set(cellShift[from]);
        else link.identity();

        link.then(shift);
        if (cellShift[to] != null) link.then(scratchShift.set(cellShift[to]).invert());

        forwards.add(new CellForward(from, cellLine[from], to, cellLine[to], link));
        cellLine[from] = ++lineCount;
        cellShift[from] = null;
        setDirty();
    }

    private void setGrid(FrameGrid grid)
    {
        this.grid = grid;
        this.byCell = new LocalFrame[grid.cellCount()];
        this.strays = new int[grid.cellCount()];
        this.cellShift = new FrameShift[grid.cellCount()];
        this.cellLine = new long[grid.cellCount()];
        forwards.clear();
        ++shiftEpoch;
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
    public LocalFrame nearest(double x, double y, double z, long tick, double radius, Vector3d offsetOut)
    {
        LocalFrame best = null;
        double bestD = radius * radius;
        Vector3d o = new Vector3d();
        for (LocalFrame f : frames)
        {
            poseOf(f, tick, 0.0, pose).toLocal(x, y, z, o);
            double d = o.x * o.x + o.y * o.y + o.z * o.z;
            if (d > bestD) continue;

            bestD = d;
            best = f;
            offsetOut.set(o);
        }
        return best;
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
        Vector3d o = shift.position(at.x, at.y, at.z, new Vector3d());
        BoundingBox3dc b = ship.boundingBox();
        double[] box = shift.bounds(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), new double[6]);
        if (box[0] - cx < -half || box[3] - cx > half || box[2] - cz < -half || box[5] - cz > half)
            throw new FrameError(String.format(Locale.ROOT, "Ship is %.0f from frame #%d, outside its cell (half %.0f)", Math.sqrt((o.x - cx) * (o.x - cx) + (o.y - cy) * (o.y - cy) + (o.z - cz) * (o.z - cz)), frame.id, half));
        if (box[1] < level.getMinBuildHeight() || box[4] > level.getMaxBuildHeight())
            throw new FrameError("Ship would land outside the height limits of frame #" + frame.id);

        FrameReport report = FrameTransfer.carry(level, List.of(ship), crewOf(level, null, ship), entitiesNear(level, ship), shift);

        refresh(level);
        Pioneer.LOGGER.info("frame {} joined #{}", FrameTransfer.shortId(ship), frame.id);
        NeoForge.EVENT_BUS.post(new FrameEvent.Joined(level, frame, ship));
        return report;
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

    public void drop(ServerLevel level, LocalFrame frame)
    {
        requireLive(frame);
        refresh(level);
        if (!frame.members.isEmpty() || !frame.players.isEmpty())
            throw new FrameError("Frame #" + frame.id + " is not empty, release it instead");
        if (frame.dormant > 0)
            throw new FrameError("Frame #" + frame.id + " has " + frame.dormant + " dormant member(s), they would be left without a frame");

        remove(level, frame);
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

        out.merged = mergeFrames(level, out);
        if (out.merged > 0) refresh(level);

        out.swept = sweepEmpty(level);
        out.frames = frames.size();
        for (int i = 0, n = frames.size(); i < n; ++i) out.dormant += frames.get(i).dormant;

        return out;
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
            boolean edge = f.edgeRecenter, ships = barycenter(f, container);
            f.edgeRecenter = false;
            f.driftX = ships ? driftX : 0.0;
            f.driftY = ships ? driftY : 0.0;
            f.driftZ = ships ? driftZ : 0.0;
            int led = ships && !edge ? lead(f, container) : FOLLOW_ALL;
            if (!ships || !(edge || due(centerX, centerY, centerZ, driftX, driftY, driftZ, distance, speed, ahead, room)))
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
            f.driftX -= driftX;
            f.driftY -= driftY;
            f.driftZ -= driftZ;
            String on = led == FOLLOW_SHIPS ? " (on the ships its players are on)" : led == FOLLOW_KEEP ? " (its players float, velocity kept)" : "";
            Pioneer.LOGGER.info("frame #{} recentered by the pass: offset {} speed {} u/s{}", f.id, String.format(Locale.ROOT, "%.1f", offset), String.format(Locale.ROOT, "%.1f", drift), on);
            ++moved;
        }

        return moved;
    }

    private int lead(LocalFrame f, ServerSubLevelContainer container)
    {
        int players = f.players.size();
        if (players == 0) return FOLLOW_ALL;
        if (carried.length < players) carried = new ServerSubLevel[2 * players];

        int count = 0;
        for (int i = 0; i < players; ++i)
        {
            ServerPlayer player = f.players.get(i);
            SubLevel ship = Sable.HELPER.getVehicleSubLevel(player);
            if (ship == null) ship = Sable.HELPER.getTrackingSubLevel(player);
            if (!(ship instanceof ServerSubLevel s) || !f.members.contains(s) || seen(s, count)) continue;

            carried[count++] = s;
        }

        if (count == 0)
        {
            driftX = driftY = driftZ = 0.0;
            return FOLLOW_KEEP;
        }

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        boolean weightless = f.mass <= 0.0;
        Vector3d first = carried[0].logicalPose().position();
        double rx = first.x, ry = first.y, rz = first.z;
        double total = 0.0, sx = 0.0, sy = 0.0, sz = 0.0, px = 0.0, py = 0.0, pz = 0.0;
        for (int i = 0; i < count; ++i)
        {
            ServerSubLevel ship = carried[i];
            carried[i] = null;
            double m = weightless ? 1.0 : FrameTransfer.massOf(ship);
            if (m <= 0.0) m = 1.0;

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

        centered(f, rx + sx / total, ry + sy / total, rz + sz / total, px / total, py / total, pz / total);
        return FOLLOW_SHIPS;
    }

    private boolean seen(ServerSubLevel ship, int count)
    {
        for (int i = 0; i < count; ++i) if (carried[i] == ship) return true;

        return false;
    }

    private boolean crosses(ServerLevel level, LocalFrame f, int g, double horizon, double margin)
    {
        int p = g * 3;
        return leaves(level, f.cell, groups.groupBox, g * 6, groups.groupVel[p] * horizon, groups.groupVel[p + 1] * horizon, groups.groupVel[p + 2] * horizon, margin);
    }

    private boolean leaves(ServerLevel level, int cell, double[] box, int b, double vx, double vy, double vz, double margin)
    {
        double cx = grid.centerX(cell), cz = grid.centerZ(cell), half = grid.halfCell() - margin;
        if (box[b] + Math.min(0.0, vx) < cx - half || box[b + 3] + Math.max(0.0, vx) > cx + half) return true;
        if (box[b + 2] + Math.min(0.0, vz) < cz - half || box[b + 5] + Math.max(0.0, vz) > cz + half) return true;

        return box[b + 1] + Math.min(0.0, vy) < level.getMinBuildHeight() || box[b + 4] + Math.max(0.0, vy) > level.getMaxBuildHeight();
    }

    private int mergeFrames(ServerLevel level, FrameMaintenance out)
    {
        int n = frames.size();
        if (n < 2) return 0;

        double merge = Config.FRAME_MERGE_DISTANCE.get(), preload = Math.max(merge, Config.FRAME_PRELOAD_DISTANCE.get());
        int period = Config.FRAME_MAINTAIN_PERIOD.get();
        double horizon = 2.0 * (period > 0 ? period / TICKS_PER_SECOND : 1.0);
        long now = level.getGameTime();
        if (spheres.length < n * SPHERE) spheres = new double[2 * n * SPHERE];

        for (int i = 0; i < n; ++i) sphere(frames.get(i), now, i * SPHERE);

        pairs = 0;
        for (int i = 0; i < n; ++i)
        {
            int a = i * SPHERE;
            if (spheres[a + 7] == 0.0) continue;

            for (int j = i + 1; j < n; ++j)
            {
                int b = j * SPHERE;
                if (spheres[b + 7] == 0.0) continue;

                double rx = spheres[b] - spheres[a], ry = spheres[b + 1] - spheres[a + 1], rz = spheres[b + 2] - spheres[a + 2];
                double reach = spheres[a + 6] + spheres[b + 6];
                double gap = closest(rx, ry, rz, spheres[b + 3] - spheres[a + 3], spheres[b + 4] - spheres[a + 4], spheres[b + 5] - spheres[a + 5], horizon) - reach;
                if (gap >= preload) continue;

                LocalFrame fa = frames.get(i), fb = frames.get(j);
                preload(level, fa, fb, gap, out);
                preload(level, fb, fa, gap, out);
                if (gap < merge) addPair(fa, fb, gap, Math.sqrt(rx * rx + ry * ry + rz * rz) - reach >= merge);
            }
        }

        sortPairs();
        int merged = 0;
        for (int p = 0; p < pairs; ++p)
        {
            LocalFrame fa = pairFrom[p], fb = pairTo[p];
            pairFrom[p] = pairTo[p] = null;
            if (fa.mergeBusy || fb.mergeBusy || !live(fa) || !live(fb)) continue;
            if (tryMerge(level, fa, fb, pairPredicted[p], horizon, out)) ++merged;
        }

        for (int i = 0, m = frames.size(); i < m; ++i)
        {
            LocalFrame f = frames.get(i);
            if (!f.mergeWaitSeen) f.mergeWaitKey = 0L;
            if (!f.preloadSeen) f.preloadKey = 0;
        }

        return merged;
    }

    private void sphere(LocalFrame f, long now, int s)
    {
        f.mergeBusy = f.mergeWaitSeen = f.preloadSeen = false;
        content.clear();
        if (!f.box.isEmpty()) content.include(f.box.minX, f.box.minY, f.box.minZ, f.box.maxX, f.box.maxY, f.box.maxZ);
        if (f.dormant > 0)
        {
            for (int r = 0, n = f.records.size(); r < n; ++r)
            {
                LocalFrame.Member m = f.records.get(r);
                if (!m.loaded) content.include(m.box.minX, m.box.minY, m.box.minZ, m.box.maxX, m.box.maxY, m.box.maxZ);
            }
        }

        if (content.isEmpty())
        {
            spheres[s + 7] = 0.0;
            return;
        }

        FrameParent parent = f.parent(system());
        double ox = 0.5 * (content.minX + content.maxX) - grid.centerX(f.cell), oy = 0.5 * (content.minY + content.maxY) - grid.centerY(), oz = 0.5 * (content.minZ + content.maxZ) - grid.centerZ(f.cell);
        mergePose.set(f.motion, parent, f.fixed, now, 0.0).toSystem(ox, oy, oz, mergePoint);
        mergePose.velocity(f.motion, parent, f.fixed, now, 0.0, mergeVelocity);
        mergePose.angularVelocity(f.motion, parent, f.fixed, now, mergeSpin);
        double rx = mergePoint.x - mergePose.px, ry = mergePoint.y - mergePose.py, rz = mergePoint.z - mergePose.pz;
        double vx = mergeVelocity.x + mergeSpin.y * rz - mergeSpin.z * ry, vy = mergeVelocity.y + mergeSpin.z * rx - mergeSpin.x * rz, vz = mergeVelocity.z + mergeSpin.x * ry - mergeSpin.y * rx;
        spheres[s] = mergePoint.x;
        spheres[s + 1] = mergePoint.y;
        spheres[s + 2] = mergePoint.z;
        if (f.members.isEmpty()) mergePoint.zero();
        else mergePose.directionToSystem(f.driftX, f.driftY, f.driftZ, mergePoint);

        spheres[s + 3] = vx * TICKS_PER_SECOND + mergePoint.x;
        spheres[s + 4] = vy * TICKS_PER_SECOND + mergePoint.y;
        spheres[s + 5] = vz * TICKS_PER_SECOND + mergePoint.z;
        spheres[s + 6] = halfDiagonal(content.minX, content.minY, content.minZ, content.maxX, content.maxY, content.maxZ);
        spheres[s + 7] = 1.0;
    }

    private void preload(ServerLevel level, LocalFrame holder, LocalFrame target, double gap, FrameMaintenance out)
    {
        if (holder.players.isEmpty() || target.dormant == 0 || target.preloadSeen) return;

        target.preloadSeen = true;
        ++out.preloading;
        for (int r = 0, n = target.records.size(); r < n; ++r)
        {
            LocalFrame.Member m = target.records.get(r);
            if (!m.loaded) ticket(level, m);
        }

        if (target.preloadKey == target.dormant) return;

        target.preloadKey = target.dormant;
        Pioneer.LOGGER.info("frame preloading #{}: {} dormant member(s), #{} with a player at {}", target.id, target.dormant, holder.id, String.format(Locale.ROOT, "%.0f", gap));
    }

    private void addPair(LocalFrame a, LocalFrame b, double gap, boolean predicted)
    {
        if (pairs == pairGap.length)
        {
            int size = 2 * pairs;
            pairFrom = Arrays.copyOf(pairFrom, size);
            pairTo = Arrays.copyOf(pairTo, size);
            pairGap = Arrays.copyOf(pairGap, size);
            pairPredicted = Arrays.copyOf(pairPredicted, size);
        }

        pairFrom[pairs] = a;
        pairTo[pairs] = b;
        pairGap[pairs] = gap;
        pairPredicted[pairs] = predicted;
        ++pairs;
    }

    private void sortPairs()
    {
        for (int i = 1; i < pairs; ++i)
        {
            LocalFrame a = pairFrom[i], b = pairTo[i];
            double gap = pairGap[i];
            boolean predicted = pairPredicted[i];
            int k = i - 1;
            for (; k >= 0 && pairGap[k] > gap; --k)
            {
                pairFrom[k + 1] = pairFrom[k];
                pairTo[k + 1] = pairTo[k];
                pairGap[k + 1] = pairGap[k];
                pairPredicted[k + 1] = pairPredicted[k];
            }

            pairFrom[k + 1] = a;
            pairTo[k + 1] = b;
            pairGap[k + 1] = gap;
            pairPredicted[k + 1] = predicted;
        }
    }

    private boolean tryMerge(ServerLevel level, LocalFrame a, LocalFrame b, boolean predicted, double horizon, FrameMaintenance out)
    {
        LocalFrame kept = keeper(a, b), from = kept == a ? b : a;
        if (kept.dormant + from.dormant > 0) return mergeWaiting(from, kept, MERGE_DORMANT, out);
        if (from.absent > 0) return mergeWaiting(from, kept, MERGE_ABSENT, out);

        FrameShift shift = shift(from, kept, level.getGameTime(), mergeShift);
        FrameBox box = from.box;
        double[] moved = shift.bounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, mergeBox);
        if (!fits(level, kept.cell, moved)) return mergeWaiting(from, kept, MERGE_CELL, out);

        if (from.members.isEmpty()) mergeVelocity.zero();
        else shift.direction(from.driftX, from.driftY, from.driftZ, mergeVelocity);

        shift.kick(0.5 * (moved[0] + moved[3]), 0.5 * (moved[1] + moved[4]), 0.5 * (moved[2] + moved[5]), mergePoint);
        double vx = (mergeVelocity.x + mergePoint.x) * horizon, vy = (mergeVelocity.y + mergePoint.y) * horizon, vz = (mergeVelocity.z + mergePoint.z) * horizon;
        if (leaves(level, kept.cell, moved, 0, vx, vy, vz, Config.FRAME_EDGE_MARGIN.get()))
            return mergeWaiting(from, kept, MERGE_EDGE, out);
        if (mainGap(level, kept, moved) > Config.FRAME_SPLIT_DISTANCE.get())
            return mergeWaiting(from, kept, MERGE_APART, out);

        List<ServerSubLevel> ships = List.copyOf(from.members);
        int players = from.players.size();
        FrameTransfer.carry(level, ships, List.copyOf(from.players), entitiesInCell(level, from.cell), shift);
        handOver(from.cell, kept.cell, shift);
        kept.mergeBusy = true;
        String how = predicted ? "predicted" : "close";
        Pioneer.LOGGER.info("frame #{} merged #{} ({}): ships={} players={}", kept.id, from.id, how, ships.size(), players);
        NeoForge.EVENT_BUS.post(new FrameEvent.Merged(level, kept, from, ships, players, predicted));
        remove(level, from);
        return true;
    }

    private double mainGap(ServerLevel level, LocalFrame kept, double[] box)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        PhysicsPipeline pipeline = container == null ? null : container.physicsSystem().getPipeline();
        groups.clear();
        for (int s = 0, n = kept.members.size(); s < n; ++s) addShip(kept.members.get(s), pipeline);

        for (int s = 0, n = kept.players.size(); s < n; ++s) addPlayer(kept.players.get(s));

        groups.link(Config.FRAME_CLUSTER_LINK.get());
        int b = mainGroup(kept) * 6;
        double[] g = groups.groupBox;
        double dx = Math.max(0.0, Math.max(g[b] - box[3], box[0] - g[b + 3]));
        double dy = Math.max(0.0, Math.max(g[b + 1] - box[4], box[1] - g[b + 4]));
        double dz = Math.max(0.0, Math.max(g[b + 2] - box[5], box[2] - g[b + 5]));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean mergeWaiting(LocalFrame from, LocalFrame kept, int reason, FrameMaintenance out)
    {
        from.mergeWaitSeen = true;
        long key = ((long) kept.id << 32) | ((long) reason << 24) | (kept.dormant + from.dormant);
        if (from.mergeWaitKey != key)
        {
            from.mergeWaitKey = key;
            from.mergeWaitText = "merge #" + from.id + " into #" + kept.id + " waiting: " + mergeReason(from, kept, reason);
            Pioneer.LOGGER.info("frame {}", from.mergeWaitText);
        }

        out.waiting.add(from.mergeWaitText);
        return false;
    }

    @Nullable
    private LocalFrame nearFrame(int g, long now)
    {
        double link = Config.FRAME_CLUSTER_LINK.get();
        int b = g * 6;
        double gx = 0.5 * (groups.groupBox[b] + groups.groupBox[b + 3]), gy = 0.5 * (groups.groupBox[b + 1] + groups.groupBox[b + 4]), gz = 0.5 * (groups.groupBox[b + 2] + groups.groupBox[b + 5]);
        double gr = halfDiagonal(groups.groupBox[b], groups.groupBox[b + 1], groups.groupBox[b + 2], groups.groupBox[b + 3], groups.groupBox[b + 4], groups.groupBox[b + 5]);
        Vector3d c = new Vector3d();
        LocalFrame best = null;
        double bestGap = link;
        for (int i = 0, n = frames.size(); i < n; ++i)
        {
            LocalFrame f = frames.get(i);
            FrameBox box = f.box;
            if (box.isEmpty()) continue;

            virtualOf(f, 0.5 * (box.minX + box.maxX), 0.5 * (box.minY + box.maxY), 0.5 * (box.minZ + box.maxZ), now, 0.0, c);
            double dx = c.x - gx, dy = c.y - gy, dz = c.z - gz;
            double gap = Math.sqrt(dx * dx + dy * dy + dz * dz) - gr - halfDiagonal(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            if (gap > bestGap) continue;

            bestGap = gap;
            best = f;
        }

        return best;
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
        Quaterniond q = FrameQuat.normalize(new Quaterniond(qx, qy, qz, qw));
        frame.motion.rebase(now).orient(q.x, q.y, q.z, q.w);
        changed(frame, FrameEvent.Cause.ORIENTATION);
    }

    public void assign(LocalFrame frame, FrameMotion motion, @Nullable FrameParent parent, boolean fixed)
    {
        requireLive(frame);
        Quaterniond q = FrameQuat.normalize(new Quaterniond(motion.qx, motion.qy, motion.qz, motion.qw));
        frame.motion.set(motion).orient(q.x, q.y, q.z, q.w);
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
        if (frame.dormant > 0)
            throw new FrameError("Frame #" + frame.id + " has " + frame.dormant + " dormant member(s), they cannot be moved");

        long now = level.getGameTime();
        Vector3d v = velocityOf(frame, now, new Vector3d());
        double speed = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z) * 20.0;
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
        return shift(from, to, now, new FrameShift());
    }

    private FrameShift shift(@Nullable LocalFrame from, @Nullable LocalFrame to, long now, FrameShift out)
    {
        FramePose a = state(from, now, shiftFrom, fromVelocity, fromSpin);
        FramePose b = state(to, now, shiftTo, toVelocity, toSpin);
        double fx = from == null ? 0.0 : grid.centerX(from.cell), fy = from == null ? 0.0 : grid.centerY(), fz = from == null ? 0.0 : grid.centerZ(from.cell);
        double tx = to == null ? 0.0 : grid.centerX(to.cell), ty = to == null ? 0.0 : grid.centerY(), tz = to == null ? 0.0 : grid.centerZ(to.cell);
        return out.between(a, fx, fy, fz, fromVelocity, fromSpin, b, tx, ty, tz, toVelocity, toSpin).scaleKick(TICKS_PER_SECOND);
    }

    private FramePose state(@Nullable LocalFrame frame, long now, FramePose out, Vector3d velocity, Vector3d spin)
    {
        if (frame == null)
        {
            velocity.zero();
            spin.zero();
            return out.identity();
        }

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

    private void saveCellLines(CompoundTag tag)
    {
        ListTag lines = new ListTag();
        for (int c = 0; c < cellLine.length; ++c)
        {
            if (cellLine[c] == 0L) continue;

            CompoundTag t = new CompoundTag();
            t.putInt("ix", grid.ix(c));
            t.putInt("iz", grid.iz(c));
            t.putLong("line", cellLine[c]);
            lines.add(t);
        }

        if (lines.isEmpty()) return;

        ListTag links = new ListTag();
        for (int i = 0, n = forwards.size(); i < n; ++i)
        {
            CellForward link = forwards.get(i);
            CompoundTag t = writeShift(link.shift(), new CompoundTag());
            t.putInt("ix", grid.ix(link.from()));
            t.putInt("iz", grid.iz(link.from()));
            t.putLong("line", link.line());
            t.putInt("tix", grid.ix(link.to()));
            t.putInt("tiz", grid.iz(link.to()));
            t.putLong("toLine", link.toLine());
            links.add(t);
        }

        tag.put("cellLines", lines);
        tag.put("cellForwards", links);
        tag.putLong("lineCount", lineCount);
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

        saveCellLines(tag);

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

    private record CellForward(int from, long line, int to, long toLine, FrameShift shift)
    {
    }
}
