package cute.ame.pioneer.SkyPlanet.Loader;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.GasDefinition;
import cute.ame.pioneer.SkyPlanet.Registry.GasRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class GasLoader extends SimplePreparableReloadListener<Map<ResourceLocation, GasDefinition>>
{
    public static final GasLoader INSTANCE = new GasLoader();
    private static final String FOLDER = "gases";
    private static final String EXT = ".json";

    @Override
    protected Map<ResourceLocation, GasDefinition> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, GasDefinition> out = new HashMap<>();
        final int prefix = FOLDER.length() + 1;

        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, rl -> rl.getPath().endsWith(EXT)).entrySet())
        {
            ResourceLocation fileRl = entry.getKey();
            String path = fileRl.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileRl.getNamespace(), path.substring(prefix, path.length() - EXT.length()));

            try (var reader = new InputStreamReader(entry.getValue().open()))
            {
                JsonElement json = GsonHelper.parse(reader);
                GasDefinition.CODEC.parse(JsonOps.INSTANCE, json)
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
    protected void apply(Map<ResourceLocation, GasDefinition> data, ResourceManager manager, ProfilerFiller profiler)
    {
        GasRegistry.replaceAll(data);
        Pioneer.LOGGER.info("[Pioneer] Applied {} gas definition(s)", data.size());
    }
}
