package cute.ame.pioneer.Gas.Loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import cute.ame.pioneer.Gas.Data.GasProperties;
import cute.ame.pioneer.Gas.Registry.GasRegistry;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class GasPropertiesLoader extends SimplePreparableReloadListener<Map<ResourceLocation, GasProperties>>
{
    public static final GasPropertiesLoader INSTANCE = new GasPropertiesLoader();
    private static final String FOLDER = "gases";
    private static final String EXT = ".json";

    @Override
    protected Map<ResourceLocation, GasProperties> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, GasProperties> out = new HashMap<>();
        final int prefix = FOLDER.length() + 1;

        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, rl -> rl.getPath().endsWith(EXT)).entrySet())
        {
            ResourceLocation fileRl = entry.getKey();
            String path = fileRl.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileRl.getNamespace(), path.substring(prefix, path.length() - EXT.length()));

            try (var reader = new InputStreamReader(entry.getValue().open()))
            {
                JsonElement json = GsonHelper.parse(reader);
                GasProperties.CODEC.parse(JsonOps.INSTANCE, json)
                    .ifSuccess(value -> out.put(id, value))
                    .ifError(err -> Pioneer.LOGGER.error("[Pioneer] Failed to parse {}/{}: {}", FOLDER, id, err.message()));
            }
            catch (Exception e)
            {
                Pioneer.LOGGER.error("[Pioneer] Error reading '{}'", fileRl, e);
            }
        }

        return out;
    }

    @Override
    protected void apply(Map<ResourceLocation, GasProperties> data, ResourceManager manager, ProfilerFiller profiler)
    {
        GasRegistry.replaceAll(data);
        Pioneer.LOGGER.info("[Pioneer] Applied {} gas properties", data.size());
    }
}
