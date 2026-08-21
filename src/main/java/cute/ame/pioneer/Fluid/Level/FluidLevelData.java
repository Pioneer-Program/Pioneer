package cute.ame.pioneer.Fluid.Level;

import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.FluidSpecies;
import cute.ame.pioneer.Fluid.SpeciesTable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FluidLevelData extends SavedData
{
    public static final String FILE_ID = "pioneer_fluids";
    private static final String K_SPECIES = "species";
    private static final String K_IDS = "ids";
    private static final String K_VOLUME = "vol";
    private static final String K_TEMPERATURE = "tmp";
    private static final String K_FLAGS = "flg";
    private static final String K_GENERATION = "gen";
    private static final String K_AMOUNT_NODE = "an";
    private static final String K_AMOUNT_SPECIES = "as";
    private static final String K_AMOUNT_VALUE = "av";

    private static final SavedData.Factory<FluidLevelData> FACTORY = new SavedData.Factory<>(FluidLevelData::new, FluidLevelData::load, null);

    private final FluidNodeStore store;

    private FluidLevelData()
    {
        this.store = new FluidNodeStore(FluidSpecies.stride());
    }

    private FluidLevelData(FluidNodeStore restored)
    {
        this.store = restored;
    }

    public static FluidLevelData get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public static @Nullable FluidLevelData getIfPresent(ServerLevel level)
    {
        return level.getDataStorage().get(FACTORY, FILE_ID);
    }

    public FluidNodeStore store()
    {
        return store;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        final int high = store.getHighWater();
        final int live = store.getLiveCount();
        final int stride = store.getStride();

        ListTag species = new ListTag();
        for (String key : FluidSpecies.active().keys()) species.add(StringTag.valueOf(key));
        tag.put(K_SPECIES, species);

        int[] ids = new int[live];
        int[] volume = new int[live];
        int[] temperature = new int[live];
        int[] flags = new int[live];
        int[] generation = new int[live];

        float[] amounts = store.getAmountsRaw();

        int n = 0;
        int entries = 0;
        for (int id = 0; id < high; id++)
        {
            if (!store.alive(id)) continue;

            ids[n] = id;
            volume[n] = Float.floatToRawIntBits(store.volume(id));
            temperature[n] = Float.floatToRawIntBits(store.temperature(id));
            flags[n] = store.flags(id);
            generation[n] = store.getGeneration(id);
            n++;

            int base = id * stride;
            for (int s = 0; s < stride; s++) if (amounts[base + s] > 0.0f) entries++;
        }

        int[] amountNode = new int[entries];
        byte[] amountSpecies = new byte[entries];
        int[] amountValue = new int[entries];

        int e = 0;
        for (int slot = 0; slot < n; slot++)
        {
            int base = ids[slot] * stride;
            for (int s = 0; s < stride; s++)
            {
                float v = amounts[base + s];
                if (v <= 0.0f) continue;

                amountNode[e] = slot;
                amountSpecies[e] = (byte) s;
                amountValue[e] = Float.floatToRawIntBits(v);
                e++;
            }
        }

        tag.putIntArray(K_IDS, ids);
        tag.putIntArray(K_VOLUME, volume);
        tag.putIntArray(K_TEMPERATURE, temperature);
        tag.putIntArray(K_FLAGS, flags);
        tag.putIntArray(K_GENERATION, generation);
        tag.putIntArray(K_AMOUNT_NODE, amountNode);
        tag.putByteArray(K_AMOUNT_SPECIES, amountSpecies);
        tag.putIntArray(K_AMOUNT_VALUE, amountValue);

        return tag;
    }

    private static FluidLevelData load(CompoundTag tag, HolderLookup.Provider registries)
    {
        int[] ids = tag.getIntArray(K_IDS);
        int[] volume = tag.getIntArray(K_VOLUME);
        int[] temperature = tag.getIntArray(K_TEMPERATURE);
        int[] flags = tag.getIntArray(K_FLAGS);
        int[] generation = tag.getIntArray(K_GENERATION);

        int live = ids.length;
        int high = 0;
        for (int id : ids) if (id + 1 > high) high = id + 1;

        FluidNodeStore store = new FluidNodeStore(FluidSpecies.stride(), Math.max(high, 64));
        store.beginRestore(high);

        for (int slot = 0; slot < live; slot++)
        {
            store.restore(
                ids[slot],
                slot < volume.length ? Float.intBitsToFloat(volume[slot]) : 0.0f,
                slot < temperature.length ? Float.intBitsToFloat(temperature[slot]) : 0.0f,
                slot < flags.length ? flags[slot] : FluidNodeStore.FLAG_ALIVE,
                slot < generation.length ? generation[slot] : 0
            );
        }

        int[] remap = FluidSpecies.active().remapFrom(savedKeys(tag));

        int[] amountNode = tag.getIntArray(K_AMOUNT_NODE);
        byte[] amountSpecies = tag.getByteArray(K_AMOUNT_SPECIES);
        int[] amountValue = tag.getIntArray(K_AMOUNT_VALUE);

        int entries = Math.min(amountNode.length, Math.min(amountSpecies.length, amountValue.length));
        for (int e = 0; e < entries; e++)
        {
            int slot = amountNode[e];
            if (slot < 0 || slot >= live) continue;

            int saved = amountSpecies[e] & 0xFF;
            int species = saved < remap.length ? remap[saved] : SpeciesTable.UNKNOWN;
            if (species == SpeciesTable.UNKNOWN) continue;

            store.add(ids[slot], species, Float.intBitsToFloat(amountValue[e]));
        }

        store.endRestore();
        return new FluidLevelData(store);
    }

    private static List<String> savedKeys(CompoundTag tag)
    {
        ListTag saved = tag.getList(K_SPECIES, Tag.TAG_STRING);
        List<String> keys = new ArrayList<>(saved.size());
        for (int i = 0; i < saved.size(); i++) keys.add(saved.getString(i));
        return keys;
    }
}
