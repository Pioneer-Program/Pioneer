package cute.ame.pioneer.Fluid.Level;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SpeciesOrder extends SavedData
{
    public static final String FILE_ID = "pioneer_species_order";
    private static final String K_ORDER = "order";

    private static final SavedData.Factory<SpeciesOrder> FACTORY = new SavedData.Factory<>(SpeciesOrder::new, SpeciesOrder::load, null);

    private final List<String> order;

    private SpeciesOrder()
    {
        this.order = new ArrayList<>();
    }

    private SpeciesOrder(List<String> order)
    {
        this.order = order;
    }

    public static SpeciesOrder get(ServerLevel anchor)
    {
        return anchor.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public List<String> keys()
    {
        return List.copyOf(order);
    }

    public List<String> merge(List<String> registryKeys)
    {
        Set<String> known = new HashSet<>(order);
        boolean changed = false;

        for (String key : registryKeys)
        {
            if (!known.add(key)) continue;
            order.add(key);
            changed = true;
        }

        if (changed) setDirty();
        return List.copyOf(order);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        ListTag list = new ListTag();
        for (String key : order) list.add(StringTag.valueOf(key));
        tag.put(K_ORDER, list);
        return tag;
    }

    private static SpeciesOrder load(CompoundTag tag, HolderLookup.Provider registries)
    {
        ListTag list = tag.getList(K_ORDER, Tag.TAG_STRING);
        List<String> order = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) order.add(list.getString(i));
        return new SpeciesOrder(order);
    }
}
