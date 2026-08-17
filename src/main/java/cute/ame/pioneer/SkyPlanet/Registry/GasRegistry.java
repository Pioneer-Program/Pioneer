package cute.ame.pioneer.SkyPlanet.Registry;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.GasDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GasRegistry
{
    private static volatile Map<String, GasDefinition> entries = Map.of();

    private static final Map<String, GasDefinition> BUILTIN = Map.ofEntries(
        Map.entry("n2", new GasDefinition(28.014, 29800.0, false, true, 0.0f, 1.00f, 0.0f, 1040.0f)),
        Map.entry("o2", new GasDefinition(31.998, 27200.0, true, true, 0.16f, 0.60f, 0.0f, 918.0f)),
        Map.entry("co2", new GasDefinition(44.009, 44900.0, false, false, 0.0f, 0.05f, 1.0f, 844.0f)),
        Map.entry("ar", new GasDefinition(39.948, 28100.0, false, true, 0.0f, 1.00f, 0.0f, 520.0f)),
        Map.entry("h2", new GasDefinition(2.016, 13800.0, false, false, 0.0f, 0.04f, 0.0f, 14300.0f)),
        Map.entry("he", new GasDefinition(4.003, 3500.0, false, false, 0.0f, 1.00f, 0.0f, 5193.0f)),
        Map.entry("ch4", new GasDefinition(16.043, 44100.0, false, false, 0.0f, 0.05f, 25.0f, 2220.0f)),
        Map.entry("h2o", new GasDefinition(18.015, 25600.0, false, true, 0.0f, 1.00f, 0.1f, 1996.0f)),
        Map.entry("nh3", new GasDefinition(17.031, 37000.0, false, false, 0.0f, 0.0025f, 0.0f, 2190.0f)),
        Map.entry("so2", new GasDefinition(64.066, 68600.0, false, false, 0.0f, 0.000002f, 0.0f, 640.0f)),
        Map.entry("ne", new GasDefinition(20.180, 6700.0, false, true, 0.0f, 1.00f, 0.0f, 1030.0f)),
        Map.entry("n2o", new GasDefinition(44.013, 51600.0, false, false, 0.0f, 0.001f, 298.0f, 880.0f))
    );

    public static void replaceAll(Map<ResourceLocation, GasDefinition> loaded)
    {
        if (loaded.isEmpty()) return;

        Map<String, GasDefinition> next = new HashMap<>(loaded.size() * 2);
        for (Map.Entry<ResourceLocation, GasDefinition> e : loaded.entrySet())
        {
            next.put(key(e.getKey().getPath()), e.getValue());
        }
        entries = Collections.unmodifiableMap(next);
    }

    public static GasDefinition get(String name)
    {
        String k = key(name);
        GasDefinition fromPack = entries.get(k);
        if (fromPack != null) return fromPack;
        return BUILTIN.getOrDefault(k, GasDefinition.FALLBACK);
    }

    public static boolean isKnown(String name)
    {
        String k = key(name);
        return entries.containsKey(k) || BUILTIN.containsKey(k);
    }

    public static Map<ResourceLocation, GasDefinition> snapshot()
    {
        Map<ResourceLocation, GasDefinition> out = new HashMap<>();
        Map<String, GasDefinition> source = entries.isEmpty() ? BUILTIN : entries;
        source.forEach((k, v) -> out.put(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, k), v));
        return out;
    }

    public static int loadedCount()
    {
        return entries.size();
    }

    private static String key(String raw)
    {
        int colon = raw.indexOf(':');
        String tail = (colon >= 0) ? raw.substring(colon + 1) : raw;
        int slash = tail.lastIndexOf('/');
        if (slash >= 0) tail = tail.substring(slash + 1);

        return tail.toLowerCase(Locale.ROOT);
    }
}
