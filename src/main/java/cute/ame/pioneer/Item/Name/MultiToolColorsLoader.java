package cute.ame.pioneer.Item.Name;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MultiToolColorsLoader extends SimplePreparableReloadListener<List<MultiToolColors.Entry>>
{
    public static final MultiToolColorsLoader INSTANCE = new MultiToolColorsLoader();
    private static final String FOLDER = "multitool_colors";
    private static final String EXT = ".json";

    @Override
    protected @NotNull List<MultiToolColors.Entry> prepare(ResourceManager manager, @NotNull ProfilerFiller profiler)
    {
        List<MultiToolColors.Entry> out = new ArrayList<>();
        Map<ResourceLocation, Resource> files = new TreeMap<>(manager.listResources(FOLDER, rl -> rl.getPath().endsWith(EXT)));

        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet())
        {
            try (var reader = new InputStreamReader(file.getValue().open()))
            {
                JsonElement json = GsonHelper.parse(reader);
                MultiToolColors.FILE.parse(JsonOps.INSTANCE, json).ifSuccess(out::addAll).ifError(err -> Pioneer.LOGGER.error("[Pioneer] Failed to parse {}: {}", file.getKey(), err.message()));
            }
            catch (Exception e)
            {
                Pioneer.LOGGER.error("[Pioneer] Error reading '{}'", file.getKey(), e);
            }
        }

        return out;
    }

    @Override
    protected void apply(@NotNull List<MultiToolColors.Entry> data, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler)
    {
        MultiToolColors.replace(data);
        Pioneer.LOGGER.info("[Pioneer] Applied {} multi-tool colours", data.size());
    }
}
