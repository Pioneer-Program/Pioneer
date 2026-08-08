package cute.ame.auralithpioneerinitiative.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record SolarSystemDefinition(
    SunDefinition sun,
    List<PlanetDefinition> planets,
    Optional<ResourceLocation> spaceDimension
)
{
    public static final Codec<SolarSystemDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            SunDefinition.CODEC.fieldOf("sun").forGetter(SolarSystemDefinition::sun),
            PlanetDefinition.CODEC.listOf().fieldOf("planets").forGetter(SolarSystemDefinition::planets),
            ResourceLocation.CODEC.optionalFieldOf("space_dimension").forGetter(SolarSystemDefinition::spaceDimension)
        ).apply(instance, SolarSystemDefinition::new)
    );

    public Optional<PlanetDefinition> findParent(ResourceLocation moonId)
    {
        for (PlanetDefinition planet : planets)
            for (PlanetDefinition moon : planet.moons())
                if (moon.id().equals(moonId)) return Optional.of(planet);

        return Optional.empty();
    }

    public Optional<PlanetDefinition> findById(ResourceLocation id)
    {
        for (PlanetDefinition planet : planets)
        {
            if (planet.id().equals(id)) return Optional.of(planet);
            for (PlanetDefinition moon : planet.moons())
                if (moon.id().equals(id)) return Optional.of(moon);

        }
        return Optional.empty();
    }

    public List<PlanetDefinition> allPlanetsFlat()
    {
        List<PlanetDefinition> flat = new java.util.ArrayList<>();
        for (PlanetDefinition planet : planets)
        {
            flat.add(planet);
            flat.addAll(planet.moons());
        }
        return flat;
    }
}