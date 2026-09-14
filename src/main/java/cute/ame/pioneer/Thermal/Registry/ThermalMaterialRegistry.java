package cute.ame.pioneer.Thermal.Registry;

import cute.ame.pioneer.Thermal.Data.ThermalMaterial;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public final class ThermalMaterialRegistry
{
    private static volatile Map<ResourceLocation, ThermalMaterial> entries = Map.of();

    public static void replaceAll(Map<ResourceLocation, ThermalMaterial> loaded)
    {
        entries = loaded.isEmpty() ? Map.of() : Map.copyOf(loaded);
    }

    public static Map<ResourceLocation, ThermalMaterial> snapshot()
    {
        return entries;
    }

    public static int loadedCount()
    {
        return entries.size();
    }
}
