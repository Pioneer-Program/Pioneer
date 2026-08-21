package cute.ame.pioneer.Fluid.Room;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.FluidConstants;
import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RoomLevelData extends SavedData
{
    public static final String FILE_ID = "pioneer_rooms";

    private static final String K_NODES = "nodes";
    private static final String K_ORIGINS = "origins";

    private static final SavedData.Factory<RoomLevelData> FACTORY = new SavedData.Factory<>(RoomLevelData::new, RoomLevelData::load, null);

    private static RoomScanner scanner;

    public record Room(int nodeId, long origin, long[] cells, boolean sealed) {}

    private final Int2ObjectOpenHashMap<Room> rooms = new Int2ObjectOpenHashMap<>();
    private final Long2IntOpenHashMap cellToNode = new Long2IntOpenHashMap();
    private final IntArrayFIFOQueue dirty = new IntArrayFIFOQueue();
    private final IntOpenHashSet queued = new IntOpenHashSet();

    private RoomLevelData()
    {
        cellToNode.defaultReturnValue(FluidNodeStore.INVALID);
    }

    public static RoomLevelData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static @Nullable RoomLevelData getIfPresent(ServerLevel level)
    {
        return level.getDataStorage().get(FACTORY, FILE_ID);
    }

    private static RoomScanner scanner()
    {
        int cap = Config.ROOM_MAX_BLOCKS.get();
        if (scanner == null || scanner.capacity() != cap) scanner = new RoomScanner(cap);

        return scanner;
    }

    public int roomCount()
    {
        return rooms.size();
    }

    public @Nullable Room room(int nodeId)
    {
        return rooms.get(nodeId);
    }

    public Iterable<Room> rooms()
    {
        return rooms.values();
    }

    public int nodeAt(BlockPos pos)
    {
        return cellToNode.get(RoomScanner.pack(pos.getX(), pos.getY(), pos.getZ()));
    }

    public int attach(ServerLevel level, BlockPos origin)
    {
        int existing = nodeAt(origin);
        if (existing != FluidNodeStore.INVALID) return existing;

        RoomScanner.Result scan = scan(level, origin);
        if (scan.isEmpty()) return FluidNodeStore.INVALID;

        FluidLevelData fluids = FluidLevelData.get(level);
        FluidNodeStore store = fluids.store();

        int nodeId = store.create((float) scan.volumeLitres(Config.ROOM_LITRES_PER_BLOCK.get()), FluidConstants.DEFAULT_TEMPERATURE_K);
        store.setFlag(nodeId, FluidNodeStore.FLAG_OPEN, !scan.sealed());

        Room room = new Room(nodeId, RoomScanner.pack(origin.getX(), origin.getY(), origin.getZ()), scan.cells(), scan.sealed());
        rooms.put(nodeId, room);
        indexCells(room);

        fluids.setDirty();
        setDirty();
        return nodeId;
    }

    public boolean remove(ServerLevel level, int nodeId)
    {
        Room room = rooms.remove(nodeId);
        if (room == null) return false;

        unindexCells(room);
        queued.remove(nodeId);

        FluidLevelData fluids = FluidLevelData.get(level);
        fluids.store().destroy(nodeId);
        fluids.setDirty();
        setDirty();
        return true;
    }

    public void onBlockChanged(BlockPos pos)
    {
        if (cellToNode.isEmpty()) return;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        markDirtyAt(RoomScanner.pack(x, y, z));
        markDirtyAt(RoomScanner.pack(x + 1, y, z));
        markDirtyAt(RoomScanner.pack(x - 1, y, z));
        markDirtyAt(RoomScanner.pack(x, y + 1, z));
        markDirtyAt(RoomScanner.pack(x, y - 1, z));
        markDirtyAt(RoomScanner.pack(x, y, z + 1));
        markDirtyAt(RoomScanner.pack(x, y, z - 1));
    }

    private void markDirtyAt(long packed)
    {
        int nodeId = cellToNode.get(packed);
        if (nodeId == FluidNodeStore.INVALID) return;

        markDirty(nodeId);
    }

    public void markDirty(int nodeId)
    {
        if (!rooms.containsKey(nodeId)) return;
        if (queued.add(nodeId)) dirty.enqueue(nodeId);
    }

    public int pendingRescans() { return queued.size(); }

    public void tick(ServerLevel level)
    {
        if (queued.isEmpty()) return;

        int budget = Config.ROOM_RESCANS_PER_TICK.get();
        while (budget-- > 0 && !dirty.isEmpty())
        {
            int nodeId = dirty.dequeueInt();
            queued.remove(nodeId);
            rescan(level, nodeId);
        }
    }

    private void rescan(ServerLevel level, int nodeId)
    {
        Room room = rooms.get(nodeId);
        if (room == null) return;

        BlockPos origin = new BlockPos(RoomScanner.unpackX(room.origin()), RoomScanner.unpackY(room.origin()), RoomScanner.unpackZ(room.origin()));
        RoomScanner.Result scan = scan(level, origin);

        if (scan.isEmpty())
        {
            remove(level, nodeId);
            return;
        }

        FluidLevelData fluids = FluidLevelData.get(level);
        FluidNodeStore store = fluids.store();
        if (!store.alive(nodeId))
        {
            rooms.remove(nodeId);
            unindexCells(room);
            setDirty();
            return;
        }

        unindexCells(room);

        Room next = new Room(nodeId, room.origin(), scan.cells(), scan.sealed());
        rooms.put(nodeId, next);
        indexCells(next);

        store.setVolume(nodeId, (float) scan.volumeLitres(Config.ROOM_LITRES_PER_BLOCK.get()));
        store.setFlag(nodeId, FluidNodeStore.FLAG_OPEN, !scan.sealed());

        fluids.setDirty();
        setDirty();
    }

    private RoomScanner.Result scan(ServerLevel level, BlockPos origin)
    {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        return scanner().scan(origin.getX(), origin.getY(), origin.getZ(),
        (x, y, z) ->
        {
            cursor.set(x, y, z);
            if (!level.hasChunkAt(cursor)) return false;

            var state = level.getBlockState(cursor);
            return state.isAir() || !state.isCollisionShapeFullBlock(level, cursor);
        },
        level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
    }

    private void indexCells(Room room)
    {
        for (long cell : room.cells()) cellToNode.put(cell, room.nodeId());
    }

    private void unindexCells(Room room)
    {
        for (long cell : room.cells()) cellToNode.remove(cell);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        int[] nodes = new int[rooms.size()];
        long[] origins = new long[rooms.size()];

        int i = 0;
        for (Room room : rooms.values())
        {
            nodes[i] = room.nodeId();
            origins[i] = room.origin();
            i++;
        }

        tag.putIntArray(K_NODES, nodes);
        tag.putLongArray(K_ORIGINS, origins);
        return tag;
    }

    private static RoomLevelData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        RoomLevelData data = new RoomLevelData();

        int[] nodes = tag.getIntArray(K_NODES);
        long[] origins = tag.getLongArray(K_ORIGINS);

        int count = Math.min(nodes.length, origins.length);
        for (int i = 0; i < count; i++)
        {
            data.rooms.put(nodes[i], new Room(nodes[i], origins[i], new long[0], false));
            data.queued.add(nodes[i]);
            data.dirty.enqueue(nodes[i]);
        }

        return data;
    }
}