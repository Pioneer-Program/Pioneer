package cute.ame.pioneer.SkyPlanet.Loader;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import cute.ame.pioneer.Core.API.AuralithAPI;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.PlanetFile;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public final class SolarSystemLoader extends SimplePreparableReloadListener<Map<ResourceLocation, SolarSystemDefinition>>
{
    public static final SolarSystemLoader INSTANCE = new SolarSystemLoader();

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLANET_FOLDER = "planets";
    private static final String SYSTEM_FOLDER = "solar_systems";
    private static final String EXT = ".json";

    @Override
    protected Map<ResourceLocation, SolarSystemDefinition> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, PlanetFile> rawPlanets = new HashMap<>();
        forEachJson(manager, PLANET_FOLDER, PlanetFile.CODEC, (fileId, file) ->
        {
            ResourceLocation planetId = PlanetDefinition.MISSING.equals(file.base().id()) ? fileId : file.base().id();
            if (rawPlanets.put(planetId, file) != null) LOGGER.warn("[Auralith] Duplicate planet '{}'", planetId);
        });

        Map<ResourceLocation, PlanetDefinition> resolved = new HashMap<>(rawPlanets.size() * 2);
        Set<ResourceLocation> visiting = new LinkedHashSet<>();
        for (ResourceLocation id : rawPlanets.keySet()) resolve(id, rawPlanets, resolved, visiting);

        Map<ResourceLocation, SolarSystemDefinition> systems = new HashMap<>();
        forEachJson(manager, SYSTEM_FOLDER, SolarSystemFile.CODEC, (systemId, file) ->
        {
            List<PlanetDefinition> planets = new ArrayList<>(file.planets().size());
            for (ResourceLocation ref : file.planets())
            {
                PlanetDefinition planet = resolved.get(ref);
                if (planet == null) LOGGER.error("[Auralith] System '{}' references unknown planet '{}'", systemId, ref);
                else planets.add(planet);
            }
            systems.put(systemId, new SolarSystemDefinition(file.sun(), List.copyOf(planets), file.spaceDimension()));
        });

        LOGGER.info("[Auralith] Prepared {} solar system(s) from {} planet file(s)", systems.size(), rawPlanets.size());
        return systems;
    }

    @Override
    protected void apply(Map<ResourceLocation, SolarSystemDefinition> data, ResourceManager manager, ProfilerFiller profiler)
    {
        AuralithAPI.clearAll();
        data.forEach(AuralithAPI::registerSolarSystem);
        LOGGER.info("[Auralith] Applied {} solar system(s)", data.size());
    }

    private static PlanetDefinition resolve(ResourceLocation id, Map<ResourceLocation, PlanetFile> raw, Map<ResourceLocation, PlanetDefinition> out, Set<ResourceLocation> visiting)
    {
        PlanetDefinition cached = out.get(id);
        if (cached != null) return cached;

        PlanetFile file = raw.get(id);
        if (file == null) return null;

        if (!visiting.add(id))
        {
            LOGGER.error("[Auralith] Cyclic moon reference involving '{}' ({})", id, visiting);
            return null;
        }

        List<ResourceLocation> refs = file.moons();
        List<PlanetDefinition> moons = List.of();

        if (!refs.isEmpty())
        {
            List<PlanetDefinition> buffer = new ArrayList<>(refs.size());
            for (ResourceLocation ref : refs)
            {
                PlanetDefinition moon = resolve(ref, raw, out, visiting);
                if (moon == null) LOGGER.error("[Auralith] Planet '{}' references unknown moon '{}'", id, ref);
                else buffer.add(moon);
            }
            moons = List.copyOf(buffer);
        }

        visiting.remove(id);
        PlanetDefinition def = file.base().withId(id).withMoons(moons);
        out.put(id, def);
        return def;
    }

    private static <T> void forEachJson(ResourceManager manager, String folder, Codec<T> codec, BiConsumer<ResourceLocation, T> sink)
    {
        final int prefix = folder.length() + 1;

        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(folder, rl -> rl.getPath().endsWith(EXT)).entrySet())
        {
            ResourceLocation fileRl = entry.getKey();
            String path = fileRl.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileRl.getNamespace(), path.substring(prefix, path.length() - EXT.length()));

            try (var reader = new InputStreamReader(entry.getValue().open()))
            {
                JsonElement json = GsonHelper.parse(reader);
                codec.parse(JsonOps.INSTANCE, json).ifSuccess(value -> sink.accept(id, value)).ifError(err -> LOGGER.error("[Auralith] Failed to parse {}/{}: {}", folder, id, err.message()));
            }
            catch (Exception e)
            {
                LOGGER.error("[Auralith] Error reading '{}'", fileRl, e);
            }
        }
    }
}
