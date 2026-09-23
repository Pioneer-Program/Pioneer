package cute.ame.pioneer.Gas.Registry;

import cute.ame.celsius.Fluid.Registry.SpeciesRegistry;
import cute.ame.pioneer.Gas.Data.GasProperties;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class GasRegistry
{
    private static volatile Map<String, GasProperties> entries = Map.of();

    private static final Map<String, GasProperties> BUILTIN = Map.ofEntries(
        Map.entry("n2", new GasProperties(29800.0, false, 0.0f, true, 0.0f, 1.00f)),
        Map.entry("o2", new GasProperties(27200.0, true, 0.0f, true, 0.16f, 0.60f)),
        Map.entry("co2", new GasProperties(44900.0, false, 1.0f, false, 0.0f, 0.05f)),
        Map.entry("ar", new GasProperties(28100.0, false, 0.0f, true, 0.0f, 1.00f)),
        Map.entry("h2", new GasProperties(13800.0, false, 0.0f, false, 0.0f, 0.04f)),
        Map.entry("he", new GasProperties(3500.0, false, 0.0f, false, 0.0f, 1.00f)),
        Map.entry("ch4", new GasProperties(44100.0, false, 25.0f, false, 0.0f, 0.05f)),
        Map.entry("h2o", new GasProperties(25600.0, false, 0.1f, true, 0.0f, 1.00f)),
        Map.entry("nh3", new GasProperties(37000.0, false, 0.0f, false, 0.0f, 0.0025f)),
        Map.entry("so2", new GasProperties(68600.0, false, 0.0f, false, 0.0f, 0.000002f)),
        Map.entry("ne", new GasProperties(6700.0, false, 0.0f, true, 0.0f, 1.00f)),
        Map.entry("n2o", new GasProperties(51600.0, false, 298.0f, false, 0.0f, 0.001f))
    );

    public static void replaceAll(Map<ResourceLocation, GasProperties> loaded)
    {
        if (loaded.isEmpty()) return;

        Map<String, GasProperties> next = new HashMap<>(loaded.size() * 2);
        for (Map.Entry<ResourceLocation, GasProperties> e : loaded.entrySet())
        {
            next.put(SpeciesRegistry.key(e.getKey().getPath()), e.getValue());
        }
        entries = Collections.unmodifiableMap(next);
    }

    public static GasProperties get(String name)
    {
        String k = SpeciesRegistry.key(name);
        GasProperties fromPack = entries.get(k);
        if (fromPack != null) return fromPack;
        return BUILTIN.getOrDefault(k, GasProperties.FALLBACK);
    }

    public static boolean isKnown(String name)
    {
        return SpeciesRegistry.isKnown(name);
    }

    public static Set<String> loadedKeys()
    {
        return entries.keySet();
    }

    public static Map<ResourceLocation, GasProperties> snapshot()
    {
        Map<ResourceLocation, GasProperties> out = new HashMap<>();
        Map<String, GasProperties> source = entries.isEmpty() ? BUILTIN : entries;
        source.forEach((k, v) -> out.put(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, k), v));
        return out;
    }
}
