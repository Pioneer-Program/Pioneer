package cute.ame.pioneer.Thermal.Registry;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Thermal.Data.ThermalDevice;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public final class ThermalDevices
{
    private static volatile Map<ResourceLocation, ThermalDevice> entries = Map.of();
    private static volatile Reference2ObjectOpenHashMap<Block, ThermalDevice> byBlock = new Reference2ObjectOpenHashMap<>(0);

    public static void replaceAll(Map<ResourceLocation, ThermalDevice> loaded)
    {
        entries = loaded.isEmpty() ? Map.of() : Map.copyOf(loaded);
    }

    public static int loadedCount()
    {
        return entries.size();
    }

    public static @Nullable ThermalDevice of(Block block)
    {
        return byBlock.get(block);
    }

    public static @Nullable ThermalDevice of(BlockState state)
    {
        return byBlock.get(state.getBlock());
    }

    public static void refresh(MinecraftServer server)
    {
        Map<ResourceLocation, ThermalDevice> raw = entries;
        HolderLookup.RegistryLookup<Block> blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        Reference2ObjectOpenHashMap<Block, ThermalDevice> index = new Reference2ObjectOpenHashMap<>(Math.max(raw.size() * 4, 8));
        for (Map.Entry<ResourceLocation, ThermalDevice> entry : raw.entrySet())
            for (String target : entry.getValue().blocks()) bind(index, entry.getValue(), target, blocks, entry.getKey());

        index.trim();
        byBlock = index;
        Pioneer.LOGGER.info("[Pioneer] Thermal devices: {} definition(s) over {} block(s)", raw.size(), index.size());
    }

    private static void bind(Reference2ObjectOpenHashMap<Block, ThermalDevice> index, ThermalDevice device, String target, HolderLookup.RegistryLookup<Block> blocks, ResourceLocation owner)
    {
        if (target.isEmpty()) return;

        boolean isTag = target.charAt(0) == '#';
        ResourceLocation id = ResourceLocation.tryParse(isTag ? target.substring(1) : target);
        if (id == null)
        {
            Pioneer.LOGGER.warn("[Pioneer] Thermal device {}: malformed target '{}'", owner, target);
            return;
        }

        if (isTag)
        {
            Optional<HolderSet.Named<Block>> tag = blocks.get(TagKey.create(Registries.BLOCK, id));
            if (tag.isEmpty())
            {
                Pioneer.LOGGER.debug("[Pioneer] Thermal device {}: empty or absent block tag #{}", owner, id);
                return;
            }

            for (Holder<Block> holder : tag.get()) index.put(holder.value(), device);
            return;
        }

        Optional<Holder.Reference<Block>> block = blocks.get(ResourceKey.create(Registries.BLOCK, id));
        if (block.isEmpty())
        {
            Pioneer.LOGGER.warn("[Pioneer] Thermal device {}: unknown block '{}'", owner, id);
            return;
        }

        index.put(block.get().value(), device);
    }
}
