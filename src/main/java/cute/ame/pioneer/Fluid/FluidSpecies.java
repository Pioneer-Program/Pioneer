package cute.ame.pioneer.Fluid;

import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Level.SpeciesOrder;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.GasDefinition;
import cute.ame.pioneer.SkyPlanet.Registry.GasRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class FluidSpecies
{
    private static volatile SpeciesTable active = SpeciesTable.EMPTY;

    public static int O2 = SpeciesTable.UNKNOWN;
    public static int CO2 = SpeciesTable.UNKNOWN;
    public static int N2 = SpeciesTable.UNKNOWN;
    public static int H2O = SpeciesTable.UNKNOWN;

    public static SpeciesTable active()
    {
        return active;
    }

    public static int stride()
    {
        return active.size();
    }

    public static void refresh(MinecraftServer server)
    {
        ServerLevel anchor = server.overworld();
        SpeciesOrder order = SpeciesOrder.get(anchor);

        List<String> keys = order.merge(registryKeys());
        SpeciesTable next = build(keys);

        if (active.sameOrderAs(keys) && active.size() == next.size())
        {
            active = next;
            resolveCommon();
            return;
        }

        SpeciesTable previous = active;
        active = next;
        resolveCommon();

        remapLoadedLevels(server, previous, next);

        Pioneer.LOGGER.info("[Pioneer] Fluid species table: {} specie(s) — {}", next.size(), String.join(", ", next.keys()));
    }

    private static void remapLoadedLevels(MinecraftServer server, SpeciesTable previous, SpeciesTable next)
    {
        if (previous.size() == 0) return;

        int[] remap = next.remapFrom(List.of(previous.keys()));
        for (ServerLevel level : server.getAllLevels())
        {
            FluidLevelData data = FluidLevelData.getIfPresent(level);
            if (data == null) continue;

            data.store().remapSpecies(remap, next.size());
            data.setDirty();
        }
    }

    private static SpeciesTable build(List<String> keys)
    {
        int n = keys.size();
        String[] array = keys.toArray(new String[0]);
        float[] molarMass = new float[n];
        float[] specificHeat = new float[n];
        boolean[] breathable = new boolean[n];
        float[] requiredPressure = new float[n];
        float[] hazardPressure = new float[n];

        for (int i = 0; i < n; i++)
        {
            GasDefinition gas = GasRegistry.get(array[i]);
            molarMass[i] = (float) gas.molarMassGPerMol();
            specificHeat[i] = gas.specificHeat();
            breathable[i] = gas.breathable();
            requiredPressure[i] = gas.requiredFraction();
            hazardPressure[i] = gas.hazardFraction();
        }

        return new SpeciesTable(array, molarMass, specificHeat, breathable, requiredPressure, hazardPressure);
    }

    private static List<String> registryKeys()
    {
        Map<ResourceLocation, GasDefinition> snapshot = GasRegistry.snapshot();
        TreeSet<String> sorted = new TreeSet<>();
        for (ResourceLocation id : snapshot.keySet()) sorted.add(id.getPath());
        return new ArrayList<>(sorted);
    }

    private static void resolveCommon()
    {
        O2 = active.indexOf("o2");
        CO2 = active.indexOf("co2");
        N2 = active.indexOf("n2");
        H2O = active.indexOf("h2o");

        if (O2 == SpeciesTable.UNKNOWN) Pioneer.LOGGER.error("[Pioneer] No specie 'o2' in Gas registries");
        if (CO2 == SpeciesTable.UNKNOWN) Pioneer.LOGGER.error("[Pioneer] No specie 'co2' in Gas registries");
    }
}
