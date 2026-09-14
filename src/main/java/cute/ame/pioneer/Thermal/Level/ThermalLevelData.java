package cute.ame.pioneer.Thermal.Level;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Thermal.Data.ThermalStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class ThermalLevelData extends SavedData
{
    public static final String FILE_ID = "pioneer_thermal";

    private static final String K_POSITIONS = "pos";
    private static final String K_KELVIN = "k";

    private static final SavedData.Factory<ThermalLevelData> FACTORY = new SavedData.Factory<>(ThermalLevelData::new, ThermalLevelData::load, null);

    private final ThermalStore store;

    private ThermalLevelData()
    {
        this.store = new ThermalStore();
    }

    private ThermalLevelData(ThermalStore restored)
    {
        this.store = restored;
    }

    public static ThermalLevelData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static @Nullable ThermalLevelData getIfPresent(ServerLevel level)
    {
        return level.getDataStorage().get(FACTORY, FILE_ID);
    }

    public ThermalStore store()
    {
        return store;
    }

    public int count()
    {
        return store.count();
    }

    public float kelvinAt(BlockPos pos)
    {
        return store.kelvinAt(pos.asLong());
    }

    public float kelvinAt(long pos)
    {
        return store.kelvinAt(pos);
    }

    public boolean attach(ServerLevel level, BlockPos pos, float kelvin)
    {
        long packed = pos.asLong();
        int slot = store.slot(packed);

        if (slot != ThermalStore.INVALID)
        {
            store.setKelvin(slot, kelvin);
            setDirty();
            return true;
        }

        float ambient = BlockTemperature.dimensionDefault(level);
        if (Math.abs(kelvin - ambient) < attachDelta()) return false;

        return force(packed, kelvin);
    }

    public boolean force(ServerLevel level, BlockPos pos, float kelvin)
    {
        return force(pos.asLong(), kelvin);
    }

    private boolean force(long pos, float kelvin)
    {
        if (store.attach(pos, kelvin, limit()) == ThermalStore.INVALID) return false;

        setDirty();
        return true;
    }

    public boolean detach(BlockPos pos)
    {
        if (!store.remove(pos.asLong())) return false;

        setDirty();
        return true;
    }

    public int settle(ServerLevel level)
    {
        int n = store.count();
        if (n == 0) return 0;

        float ambient = BlockTemperature.dimensionDefault(level);
        float detach = detachDelta();

        long[] positions = store.positionsRaw();
        float[] kelvin = store.kelvinRaw();

        for (int slot = 0; slot < n; slot++)
        {
            if (Math.abs(kelvin[slot] - ambient) > detach) continue;

            store.markDetached(positions[slot]);
        }

        int removed = store.flushDetached();
        if (removed > 0) setDirty();
        return removed;
    }

    public static void onMaterialsReloaded(MinecraftServer server)
    {
        for (ServerLevel level : server.getAllLevels())
        {
            ThermalLevelData data = getIfPresent(level);
            if (data != null) data.store.invalidateMaterials();
        }
    }

    private static float attachDelta()
    {
        return Config.THERMAL_ATTACH_DELTA_K.get().floatValue();
    }

    private static float detachDelta()
    {
        float attach = attachDelta();
        return Math.min(Config.THERMAL_DETACH_DELTA_K.get().floatValue(), attach);
    }

    private static int limit()
    {
        return Config.THERMAL_MAX_ENTRIES.get();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        int n = store.count();
        long[] positions = Arrays.copyOf(store.positionsRaw(), n);
        int[] kelvin = new int[n];
        float[] raw = store.kelvinRaw();
        for (int slot = 0; slot < n; slot++) kelvin[slot] = Float.floatToRawIntBits(raw[slot]);

        tag.putLongArray(K_POSITIONS, positions);
        tag.putIntArray(K_KELVIN, kelvin);
        return tag;
    }

    private static ThermalLevelData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        long[] positions = tag.getLongArray(K_POSITIONS);
        int[] kelvin = tag.getIntArray(K_KELVIN);
        int entries = Math.min(positions.length, kelvin.length);

        ThermalStore store = new ThermalStore(Math.max(entries, 64));
        store.beginRestore(entries);
        for (int i = 0; i < entries; i++) store.restore(positions[i], Float.intBitsToFloat(kelvin[i]));

        store.endRestore();
        return new ThermalLevelData(store);
    }
}