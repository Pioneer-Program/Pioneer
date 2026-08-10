package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record CloudsDefinition(
    Optional<ResourceLocation> texture,
    float scale,
    float rotationSpeed,
    List<Float> color,
    float innerAltitude,
    float outerAltitude,
    float coverage,
    float density,
    float noiseScale,
    float erosion,
    float windSpeed,
    List<Float> windDirection,
    float mieG,
    float sunIntensity,
    float multiScatterStrength,
    float powderStrength
)
{
    public static final Codec<CloudsDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(CloudsDefinition::texture),
            Codec.FLOAT.optionalFieldOf("scale", 1.06f).forGetter(CloudsDefinition::scale),
            Codec.FLOAT.optionalFieldOf("rotation_speed", 0.3f).forGetter(CloudsDefinition::rotationSpeed),
            Codec.FLOAT.listOf().optionalFieldOf("color", List.of(1.0f, 1.0f, 1.0f)).forGetter(CloudsDefinition::color),
            Codec.FLOAT.optionalFieldOf("inner_altitude", 0.015f).forGetter(CloudsDefinition::innerAltitude),
            Codec.FLOAT.optionalFieldOf("outer_altitude", 0.05f).forGetter(CloudsDefinition::outerAltitude),
            Codec.FLOAT.optionalFieldOf("coverage", 0.5f).forGetter(CloudsDefinition::coverage),
            Codec.FLOAT.optionalFieldOf("density", 1.0f).forGetter(CloudsDefinition::density),
            Codec.FLOAT.optionalFieldOf("noise_scale", 6.0f).forGetter(CloudsDefinition::noiseScale),
            Codec.FLOAT.optionalFieldOf("erosion", 0.55f).forGetter(CloudsDefinition::erosion),
            Codec.FLOAT.optionalFieldOf("wind_speed", 1.0f).forGetter(CloudsDefinition::windSpeed),
            Codec.FLOAT.listOf().optionalFieldOf("wind_direction", List.of(1.0f, 0.0f)).forGetter(CloudsDefinition::windDirection),
            Codec.FLOAT.optionalFieldOf("mie_g", 0.85f).forGetter(CloudsDefinition::mieG),
            Codec.FLOAT.optionalFieldOf("sun_intensity", 6.0f).forGetter(CloudsDefinition::sunIntensity),
            Codec.FLOAT.optionalFieldOf("multi_scatter_strength", 0.4f).forGetter(CloudsDefinition::multiScatterStrength),
            Codec.FLOAT.optionalFieldOf("powder_strength", 1.0f).forGetter(CloudsDefinition::powderStrength)
        ).apply(instance, CloudsDefinition::new)
    );

    public float r() { return get(0, 1.0f); }
    public float g() { return get(1, 1.0f); }
    public float b() { return get(2, 1.0f); }

    public float windX() { return getWind(0, 1.0f); }
    public float windZ() { return getWind(1, 0.0f); }

    private float get(int idx, float fallback)
    {
        return (color != null && color.size() > idx) ? color.get(idx) : fallback;
    }

    private float getWind(int idx, float fallback)
    {
        return (windDirection != null && windDirection.size() > idx) ? windDirection.get(idx) : fallback;
    }
}
