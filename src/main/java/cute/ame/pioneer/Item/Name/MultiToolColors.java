package cute.ame.pioneer.Item.Name;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.pioneer.Pioneer;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class MultiToolColors
{
    public static final TagKey<Block> UNNAMEABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_unnameable"));

    public static final Codec<Integer> RGB = Codec.STRING.comapFlatMap(MultiToolColors::parse, rgb -> String.format("#%06X", rgb));
    public static final Codec<Entry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(Codec.STRING.fieldOf("target").forGetter(Entry::target), RGB.fieldOf("color").forGetter(Entry::rgb)).apply(instance, Entry::new));
    public static final Codec<List<Entry>> FILE = ENTRY.listOf().fieldOf("values").codec();
    private static final Reference2IntOpenHashMap<Block> CACHE = new Reference2IntOpenHashMap<>();
    private static volatile List<Entry> entries = List.of();
    private static volatile int version;
    private static int cachedVersion = -1;

    public static List<Entry> entries()
    {
        return entries;
    }

    public static void replace(List<Entry> next)
    {
        entries = List.copyOf(next);
        invalidate();
    }

    public static void invalidate()
    {
        version++;
    }

    public static int colorOf(BlockState state)
    {
        if (cachedVersion != version)
        {
            CACHE.clear();
            cachedVersion = version;
        }

        Block block = state.getBlock();
        if (CACHE.containsKey(block)) return CACHE.getInt(block);

        int rgb = resolve(state);
        CACHE.put(block, rgb);
        return rgb;
    }

    private static int resolve(BlockState state)
    {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String key = id.toString();
        List<Entry> list = entries;

        for (int i = list.size() - 1; i >= 0; i--)
        {
            Entry entry = list.get(i);
            String target = entry.target();

            if (target.startsWith("#"))
            {
                ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
                if (tag != null && state.is(TagKey.create(Registries.BLOCK, tag))) return entry.rgb();
            }
            else if (target.equals(key)) return entry.rgb();
        }

        float hue = (id.hashCode() & 0xFFFF) / 65536.0f;
        return Mth.hsvToRgb(hue, 0.6f, 1.0f) & 0xFFFFFF;
    }

    private static DataResult<Integer> parse(String text)
    {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6) return DataResult.error(() -> "Colour must be #RRGGBB: " + text);

        try
        {
            return DataResult.success(Integer.parseInt(hex, 16));
        }
        catch (NumberFormatException e)
        {
            return DataResult.error(() -> "Colour must be #RRGGBB: " + text);
        }
    }

    public record Entry(String target, int rgb)
    {
    }
}
