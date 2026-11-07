package cute.ame.pioneer.LifeSupport.Level;

import cute.ame.celsius.Core.Thermal.Residency;
import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Physics.RoomScanner;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Block.VentBlock;
import cute.ame.pioneer.LifeSupport.Physics.Photosynthesis;
import cute.ame.pioneer.Pioneer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class KelpBeds
{
    public static final TagKey<Block> PHOTOSYNTHETIC = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "photosynthetic"));

    private static final Reference2ObjectOpenHashMap<ServerLevel, KelpBeds> BY_LEVEL = new Reference2ObjectOpenHashMap<>();
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();
    private static final LongArrayList OWNED = new LongArrayList();
    private static final LongArrayList PLANTS = new LongArrayList();
    private static final long[] NONE = new long[0];

    private static RoomScanner scanner;
    private static int tracked;

    public static final class Bed
    {
        private final ObjectArrayList<FluidVesselBlockEntity> vents = new ObjectArrayList<>(1);
        private long[] cells = NONE;
        private long[] samples = NONE;
        private int plants;
        private boolean dirty;

        public int plants()
        {
            return plants;
        }

        public int cells()
        {
            return cells.length;
        }

        public int vents()
        {
            return vents.size();
        }

        public BlockPos vent(int index)
        {
            return vents.get(index).getBlockPos();
        }
    }

    private final Long2ObjectOpenHashMap<Bed> cellToBed = new Long2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<FluidVesselBlockEntity, Bed> ventToBed = new Reference2ObjectOpenHashMap<>();
    private final ObjectArrayList<Bed> beds = new ObjectArrayList<>();
    private final ObjectArrayList<Bed> dirty = new ObjectArrayList<>();
    private final ObjectArrayFIFOQueue<FluidVesselBlockEntity> pending = new ObjectArrayFIFOQueue<>();
    private final ObjectArrayList<FluidVesselBlockEntity> waiting = new ObjectArrayList<>();

    public static @Nullable KelpBeds getIfPresent(ServerLevel level)
    {
        return BY_LEVEL.get(level);
    }

    private static KelpBeds get(ServerLevel level)
    {
        KelpBeds beds = BY_LEVEL.get(level);
        if (beds != null) return beds;

        beds = new KelpBeds();
        BY_LEVEL.put(level, beds);
        return beds;
    }

    public static boolean anyTracked()
    {
        return tracked != 0;
    }

    public static void onVentLoaded(FluidVesselBlockEntity vent)
    {
        if (!(vent.getLevel() instanceof ServerLevel level)) return;

        get(level).pending.enqueue(vent);
    }

    public static void onVentRemoved(FluidVesselBlockEntity vent)
    {
        if (!(vent.getLevel() instanceof ServerLevel level)) return;

        KelpBeds beds = BY_LEVEL.get(level);
        if (beds != null) beds.detach(vent);
    }

    public static void onBlockChanged(ServerLevel level, BlockPos pos)
    {
        KelpBeds beds = BY_LEVEL.get(level);
        if (beds == null || beds.cellToBed.isEmpty()) return;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        beds.markAt(RoomScanner.pack(x, y, z));
        beds.markAt(RoomScanner.pack(x + 1, y, z));
        beds.markAt(RoomScanner.pack(x - 1, y, z));
        beds.markAt(RoomScanner.pack(x, y + 1, z));
        beds.markAt(RoomScanner.pack(x, y - 1, z));
        beds.markAt(RoomScanner.pack(x, y, z + 1));
        beds.markAt(RoomScanner.pack(x, y, z - 1));
    }

    public static void unload(ServerLevel level)
    {
        KelpBeds beds = BY_LEVEL.remove(level);
        if (beds != null) tracked -= beds.cellToBed.size();
    }

    public static void clear()
    {
        BY_LEVEL.clear();
        tracked = 0;
    }

    public List<Bed> beds()
    {
        return beds;
    }

    public int pendingCount()
    {
        return pending.size() + waiting.size();
    }

    public void tick(ServerLevel level)
    {
        if (!dirty.isEmpty())
        {
            for (Bed bed : dirty) dissolve(bed);

            dirty.clear();
        }

        int period = Config.KELP_PERIOD_TICKS.get();
        boolean periodic = level.getGameTime() % period == 0;
        if (periodic && !waiting.isEmpty())
        {
            for (FluidVesselBlockEntity fluidVesselBlockEntity : waiting) pending.enqueue(fluidVesselBlockEntity);
            waiting.clear();
        }

        if (!pending.isEmpty()) settle(level);
        if (periodic && !beds.isEmpty()) photosynthesize(level, period);
    }

    public float light(ServerLevel level, Bed bed)
    {
        long[] samples = bed.samples;
        float sum = 0.0f;
        int counted = 0;
        for (long sample : samples)
        {
            CURSOR.set(RoomScanner.unpackX(sample), RoomScanner.unpackY(sample), RoomScanner.unpackZ(sample));
            if (!Residency.isLoaded(level, CURSOR)) continue;

            sum += Photosynthesis.light(level.getMaxLocalRawBrightness(CURSOR));
            counted++;
        }

        return counted == 0 ? 0.0f : sum / counted;
    }

    private void photosynthesize(ServerLevel level, int period)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        SpeciesTable table = FluidSpecies.active();
        int co2 = FluidSpecies.CO2;
        int o2 = FluidSpecies.O2;
        if (!table.isValid(co2) || !table.isValid(o2)) return;

        FluidNodeStore store = data.store();
        float rate = Config.KELP_MOL_PER_TICK.get().floatValue();
        float floorPressure = Config.KELP_CO2_FLOOR_P.get().floatValue();

        for (Bed bed : beds)
        {
            if (bed.plants == 0 || bed.dirty) continue;

            ObjectArrayList<FluidVesselBlockEntity> vents = bed.vents;
            int count = vents.size();
            float share = -1.0f;

            for (FluidVesselBlockEntity vent : vents)
            {
                int node = store.resolve(vent.getNodeHandle());
                if (node == FluidNodeStore.INVALID || store.isLiquid(node)) continue;

                float amount = store.amount(node, co2);
                if (amount <= 0.0f) continue;

                float floor = Photosynthesis.floorMoles(store.moles(node), store.pressure(node), floorPressure);
                if (amount <= floor) continue;

                if (share < 0.0f) share = Photosynthesis.capacity(bed.plants, light(level, bed), rate, period) / count;
                if (share <= 0.0f) break;

                float converted = Photosynthesis.convertible(amount, floor, share);
                if (converted <= 0.0f) continue;

                store.add(node, co2, -converted);
                store.add(node, o2, converted);
                data.touch(node);
            }
        }
    }

    private void settle(ServerLevel level)
    {
        int budget = Config.KELP_RESCANS_PER_TICK.get();
        while (!pending.isEmpty())
        {
            FluidVesselBlockEntity vent = pending.first();
            BlockState state = vent.getBlockState();
            if (vent.isRemoved() || ventToBed.containsKey(vent) || !(state.getBlock() instanceof VentBlock))
            {
                pending.dequeue();
                continue;
            }

            BlockPos mouth = VentBlock.mouth(vent.getBlockPos(), state);
            Bed owner = cellToBed.get(RoomScanner.pack(mouth.getX(), mouth.getY(), mouth.getZ()));
            if (owner != null)
            {
                if (owner.dirty)
                {
                    dissolve(owner);
                    continue;
                }

                pending.dequeue();
                join(owner, vent);
                continue;
            }

            if (!Residency.isLoaded(level, mouth))
            {
                pending.dequeue();
                waiting.add(vent);
                continue;
            }

            if (budget-- <= 0) return;

            pending.dequeue();
            join(scan(level, mouth), vent);
        }
    }

    private Bed scan(ServerLevel level, BlockPos mouth)
    {
        Bed bed = new Bed();
        long origin = RoomScanner.pack(mouth.getX(), mouth.getY(), mouth.getZ());
        RoomScanner.Result result = scanner().scan(mouth.getX(), mouth.getY(), mouth.getZ(), (x, y, z) ->
        {
            CURSOR.set(x, y, z);
            return Residency.isLoaded(level, CURSOR) && holdsBed(level.getBlockState(CURSOR));
        },

        level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        if (result.isEmpty())
        {
            bed.cells = new long[] { origin };
            index(origin, bed);
            beds.add(bed);
            return bed;
        }

        OWNED.clear();
        PLANTS.clear();
        for (long cell : result.cells())
        {
            if (cellToBed.containsKey(cell)) continue;

            index(cell, bed);
            OWNED.add(cell);

            CURSOR.set(RoomScanner.unpackX(cell), RoomScanner.unpackY(cell), RoomScanner.unpackZ(cell));
            if (level.getBlockState(CURSOR).is(PHOTOSYNTHETIC)) PLANTS.add(cell);
        }

        bed.cells = OWNED.toLongArray();
        bed.plants = PLANTS.size();
        bed.samples = sample(PLANTS, Config.KELP_LIGHT_SAMPLES.get());
        beds.add(bed);
        return bed;
    }

    private static long[] sample(LongArrayList plants, int limit)
    {
        int total = plants.size();
        if (total == 0) return NONE;

        int n = Math.min(limit, total);
        long[] samples = new long[n];
        for (int i = 0; i < n; i++) samples[i] = plants.getLong((int) ((long) i * total / n));

        return samples;
    }

    private static boolean holdsBed(BlockState state)
    {
        if (state.is(PHOTOSYNTHETIC)) return true;
        if (state.getBlock() instanceof FluidVesselBlock) return false;

        FluidState fluid = state.getFluidState();
        return fluid.isSource() && fluid.is(FluidTags.WATER);
    }

    private void join(Bed bed, FluidVesselBlockEntity vent)
    {
        bed.vents.add(vent);
        ventToBed.put(vent, bed);
    }

    private void detach(FluidVesselBlockEntity vent)
    {
        Bed bed = ventToBed.remove(vent);
        if (bed == null) return;

        bed.vents.remove(vent);
        if (!bed.vents.isEmpty()) return;

        beds.remove(bed);
        unindex(bed);
    }

    private void dissolve(Bed bed)
    {
        if (!beds.remove(bed)) return;

        unindex(bed);

        ObjectArrayList<FluidVesselBlockEntity> vents = bed.vents;
        for (FluidVesselBlockEntity vent : vents)
        {
            ventToBed.remove(vent);
            pending.enqueue(vent);
        }

        vents.clear();
    }

    private void markAt(long cell)
    {
        Bed bed = cellToBed.get(cell);
        if (bed == null || bed.dirty) return;

        bed.dirty = true;
        dirty.add(bed);
    }

    private void index(long cell, Bed bed)
    {
        cellToBed.put(cell, bed);
        tracked++;
    }

    private void unindex(Bed bed)
    {
        for (long cell : bed.cells)
        {
            if (cellToBed.get(cell) != bed) continue;

            cellToBed.remove(cell);
            tracked--;
        }
    }

    private static RoomScanner scanner()
    {
        int cap = Config.KELP_BED_MAX_BLOCKS.get();
        if (scanner == null || scanner.capacity() != cap) scanner = new RoomScanner(cap);

        return scanner;
    }
}
