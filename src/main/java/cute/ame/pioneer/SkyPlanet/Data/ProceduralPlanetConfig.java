package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record ProceduralPlanetConfig(
    PlanetType type,
    long seed,
    List<Float> primaryColor,
    List<Float> secondaryColor,
    int resolution,
    int octaves,
    float roughness,
    Optional<ResourceLocation> generatorOverride
)
{
    public static final Codec<ProceduralPlanetConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            PlanetType.CODEC.fieldOf("type").forGetter(ProceduralPlanetConfig::type),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(ProceduralPlanetConfig::seed),
            Codec.FLOAT.listOf().optionalFieldOf("primary_color",   List.of(0.6f, 0.5f, 0.4f)).forGetter(ProceduralPlanetConfig::primaryColor),
            Codec.FLOAT.listOf().optionalFieldOf("secondary_color", List.of(0.3f, 0.3f, 0.3f)).forGetter(ProceduralPlanetConfig::secondaryColor),
            Codec.INT.optionalFieldOf("resolution", 256).forGetter(ProceduralPlanetConfig::resolution),
            Codec.INT.optionalFieldOf("octaves", 8).forGetter(ProceduralPlanetConfig::octaves),
            Codec.FLOAT.optionalFieldOf("roughness", 0.55f).forGetter(ProceduralPlanetConfig::roughness),
            ResourceLocation.CODEC.optionalFieldOf("generator").forGetter(ProceduralPlanetConfig::generatorOverride)
        ).apply(instance, ProceduralPlanetConfig::new)
    );

    public float pr() { return get(primaryColor, 0, 0.6f); }
    public float pg() { return get(primaryColor, 1, 0.5f); }
    public float pb() { return get(primaryColor, 2, 0.4f); }
    public float sr() { return get(secondaryColor, 0, 0.3f); }
    public float sg() { return get(secondaryColor, 1, 0.3f); }
    public float sb() { return get(secondaryColor, 2, 0.3f); }

    public ResourceLocation generatorId()
    {
        return generatorOverride.orElseGet(type::defaultGeneratorId);
    }

    private static float get(List<Float> list, int idx, float fallback)
    {
        return (list != null && list.size() > idx) ? list.get(idx) : fallback;
    }
}
