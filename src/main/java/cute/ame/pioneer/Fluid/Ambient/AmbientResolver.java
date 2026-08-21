package cute.ame.pioneer.Fluid.Ambient;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Fluid.FluidSpecies;
import cute.ame.pioneer.Fluid.SpeciesTable;
import cute.ame.pioneer.SkyPlanet.Data.AtmosphereDefinition;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.pioneer.SkyPlanet.Physics.PlanetEnvironment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AmbientResolver
{
    private static final double BAR_TO_P = 0.986923;
    private static final float DEFAULT_TEMPERATURE_K = 288.15f;

    private static final Map<ResourceLocation, AmbientState> CACHE = new HashMap<>();

    public static AmbientState of(ResourceKey<Level> dimension)
    {
        AmbientState cached = CACHE.get(dimension.location());
        if (cached != null) return cached;

        AmbientState resolved = resolve(dimension);
        CACHE.put(dimension.location(), resolved);
        return resolved;
    }

    public static void invalidate()
    {
        CACHE.clear();
    }

    private static AmbientState resolve(ResourceKey<Level> dimension)
    {
        SpeciesTable table = FluidSpecies.active();

        Optional<PioneerAPI.DimensionBinding> bindingOpt = PioneerAPI.getBindingForDimension(dimension);
        if (bindingOpt.isEmpty()) return earthLike(table);

        PioneerAPI.DimensionBinding binding = bindingOpt.get();
        if (binding.isSpaceDimension()) return AmbientState.vacuum(table.size());

        Optional<SolarSystemDefinition> systemOpt = PioneerAPI.getSolarSystem(binding.systemId());
        if (systemOpt.isEmpty()) return earthLike(table);

        SolarSystemDefinition system = systemOpt.get();
        Optional<PlanetDefinition> planetOpt = system.findById(binding.planetId());
        if (planetOpt.isEmpty()) return earthLike(table);

        PlanetDefinition planet = planetOpt.get();
        Optional<AtmosphereDefinition> atmoOpt = planet.atmosphere();
        if (atmoOpt.isEmpty()) return AmbientState.vacuum(table.size());

        AtmosphereDefinition atmo = atmoOpt.get();
        double pressure = Math.max(atmo.surfacePressureBar(), 0.0f) * BAR_TO_P;
        if (pressure < AmbientState.VACUUM_THRESHOLD_P) return AmbientState.vacuum(table.size());

        return new AmbientState(pressure, surfaceTemperature(system, planet), fractions(table, atmo), false);
    }

    private static float surfaceTemperature(SolarSystemDefinition system, PlanetDefinition planet)
    {
        Optional<PlanetDefinition> parent = system.findParent(planet.id());

        PlanetEnvironment env = parent
            .map(p -> PlanetEnvironment.ofMoon(system.sun(), p, planet))
            .orElseGet(() -> PlanetEnvironment.of(system.sun(), planet));

        double kelvin = env.surfaceTempK();
        return (kelvin > 1.0 && kelvin < 5000.0) ? (float) kelvin : DEFAULT_TEMPERATURE_K;
    }

    private static float[] fractions(SpeciesTable table, AtmosphereDefinition atmo)
    {
        float[] fractions = new float[table.size()];

        double total = 0.0;
        for (Map.Entry<String, Float> entry : atmo.composition().entrySet())
        {
            int species = table.indexOf(entry.getKey());
            if (!table.isValid(species)) continue;

            float value = Math.max(entry.getValue(), 0.0f);
            fractions[species] += value;
            total += value;
        }

        if (total <= 1.0e-9) return fractions;

        float inverse = (float) (1.0 / total);
        for (int s = 0; s < fractions.length; s++) fractions[s] *= inverse;
        return fractions;
    }

    private static AmbientState earthLike(SpeciesTable table)
    {
        float[] fractions = new float[table.size()];

        double total = 0.0;
        for (Map.Entry<String, Float> entry : AtmosphereDefinition.EARTH_LIKE.entrySet())
        {
            int species = table.indexOf(entry.getKey());
            if (!table.isValid(species)) continue;

            fractions[species] += entry.getValue();
            total += entry.getValue();
        }

        if (total > 1.0e-9)
        {
            float inverse = (float) (1.0 / total);
            for (int s = 0; s < fractions.length; s++) fractions[s] *= inverse;
        }

        return new AmbientState(BAR_TO_P, DEFAULT_TEMPERATURE_K, fractions, false);
    }
}
