package cute.ame.pioneer.Thermal.Registry;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Thermal.Data.MaterialTable;
import cute.ame.pioneer.Thermal.Data.ThermalMaterial;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ThermalMaterials
{
    private static final class Columns
    {
        private final String[] keys;
        private final float[] conductivity;
        private final float[] volumetricHeat;
        private final float[] breakdownK;
        private final float[] emissivity;
        private final float[] areaFactor;
        private final BlockState[] into;

        private Columns(int count)
        {
            keys = new String[count];
            conductivity = new float[count];
            volumetricHeat = new float[count];
            breakdownK = new float[count];
            emissivity = new float[count];
            areaFactor = new float[count];
            into = new BlockState[count];
        }

        private MaterialTable toTable()
        {
            return new MaterialTable(keys, conductivity, volumetricHeat, breakdownK, emissivity, areaFactor);
        }
    }

    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "default");

    private static final Comparator<Map.Entry<ResourceLocation, ThermalMaterial>> ORDER = Comparator.<Map.Entry<ResourceLocation, ThermalMaterial>>comparingInt(e -> e.getValue().priority()).thenComparing(Map.Entry::getKey);
    private static volatile MaterialTable table = MaterialTable.EMPTY;
    private static volatile Reference2IntOpenHashMap<Block> byBlock = emptyIndex();
    private static volatile BlockState[] breakdownInto = { Blocks.AIR.defaultBlockState() };

    public static MaterialTable table()
    {
        return table;
    }

    public static int indexOf(Block block)
    {
        return byBlock.getInt(block);
    }

    public static int indexOf(BlockState state)
    {
        return byBlock.getInt(state.getBlock());
    }

    public static BlockState breakdownInto(int material)
    {
        BlockState[] states = breakdownInto;
        return material >= 0 && material < states.length ? states[material] : Blocks.AIR.defaultBlockState();
    }

    public static int coveredBlocks()
    {
        return byBlock.size();
    }

    public static void refresh(MinecraftServer server)
    {
        Map<ResourceLocation, ThermalMaterial> raw = ThermalMaterialRegistry.snapshot();
        HolderLookup.RegistryLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);

        List<Map.Entry<ResourceLocation, ThermalMaterial>> ordered = new ArrayList<>(raw.size());
        for (Map.Entry<ResourceLocation, ThermalMaterial> entry : raw.entrySet())
        {
            if (!DEFAULT_ID.equals(entry.getKey())) ordered.add(entry);
        }
        ordered.sort(ORDER);

        int count = ordered.size() + 1;
        Columns columns = new Columns(count);

        Reference2IntOpenHashMap<Block> index = new Reference2IntOpenHashMap<>(Math.max(raw.size() << 3, 64));
        index.defaultReturnValue(MaterialTable.DEFAULT);

        ThermalMaterial fallback = raw.getOrDefault(DEFAULT_ID, ThermalMaterial.FALLBACK);
        columns.keys[MaterialTable.DEFAULT] = DEFAULT_ID.toString();
        write(MaterialTable.DEFAULT, fallback, blocks, columns);
        bindAll(index, MaterialTable.DEFAULT, fallback, blocks, DEFAULT_ID);

        for (int i = 0; i < ordered.size(); i++)
        {
            Map.Entry<ResourceLocation, ThermalMaterial> entry = ordered.get(i);
            ThermalMaterial material = entry.getValue();
            int slot = i + 1;

            columns.keys[slot] = entry.getKey().toString();
            write(slot, material, blocks, columns);
            bindAll(index, slot, material, blocks, entry.getKey());
        }

        index.trim();

        table = columns.toTable();
        breakdownInto = columns.into;
        byBlock = index;

        Pioneer.LOGGER.info("[Pioneer] Thermal material table: {} material(s) over {} block(s)", count, index.size());
    }

    private static void write(int slot, ThermalMaterial material, HolderLookup.RegistryLookup<Block> blocks, Columns columns)
    {
        columns.conductivity[slot] = material.conductivity();
        columns.volumetricHeat[slot] = material.volumetricHeat();
        columns.breakdownK[slot] = material.breakdown();
        columns.emissivity[slot] = material.emissivity();
        columns.areaFactor[slot] = material.areaFactor();
        columns.into[slot] = resolveState(material.breakdownResult(), blocks);
    }

    private static BlockState resolveState(Optional<String> id, HolderLookup.RegistryLookup<Block> blocks)
    {
        if (id.isEmpty()) return Blocks.AIR.defaultBlockState();

        ResourceLocation parsed = ResourceLocation.tryParse(id.get());
        if (parsed == null)
        {
            Pioneer.LOGGER.warn("[Pioneer] Thermal material: malformed breakdown_result '{}'", id.get());
            return Blocks.AIR.defaultBlockState();
        }

        return blocks.get(ResourceKey.create(Registries.BLOCK, parsed)).map(holder -> holder.value().defaultBlockState()).orElseGet(() ->
        {
            Pioneer.LOGGER.warn("[Pioneer] Thermal material: unknown breakdown_result '{}'", parsed);
            return Blocks.AIR.defaultBlockState();
        });
    }

    private static void bindAll(Reference2IntOpenHashMap<Block> index, int slot, ThermalMaterial material, HolderLookup.RegistryLookup<Block> blocks, ResourceLocation owner)
    {
        for (String target : material.blocks()) bind(index, slot, target, blocks, owner);
    }

    private static void bind(Reference2IntOpenHashMap<Block> index, int slot, String target, HolderLookup.RegistryLookup<Block> blocks, ResourceLocation owner)
    {
        if (target.isEmpty()) return;

        boolean isTag = target.charAt(0) == '#';
        ResourceLocation id = ResourceLocation.tryParse(isTag ? target.substring(1) : target);

        if (id == null)
        {
            Pioneer.LOGGER.warn("[Pioneer] Thermal material {}: malformed target '{}'", owner, target);
            return;
        }

        if (isTag)
        {
            Optional<HolderSet.Named<Block>> tag = blocks.get(TagKey.create(Registries.BLOCK, id));
            if (tag.isEmpty())
            {
                Pioneer.LOGGER.debug("[Pioneer] Thermal material {}: empty or absent block tag #{}", owner, id);
                return;
            }

            for (Holder<Block> holder : tag.get()) index.put(holder.value(), slot);
            return;
        }

        Optional<Holder.Reference<Block>> block = blocks.get(ResourceKey.create(Registries.BLOCK, id));
        if (block.isEmpty())
        {
            Pioneer.LOGGER.warn("[Pioneer] Thermal material {}: unknown block '{}'", owner, id);
            return;
        }

        index.put(block.get().value(), slot);
    }

    private static Reference2IntOpenHashMap<Block> emptyIndex()
    {
        Reference2IntOpenHashMap<Block> index = new Reference2IntOpenHashMap<>(0);
        index.defaultReturnValue(MaterialTable.DEFAULT);
        return index;
    }
}
