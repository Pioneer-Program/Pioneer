package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record SunDefinition(
    float size,
    float radiusSolar,
    float massSolar,
    float axialRotationSpeed,
    List<Float> glowColor,
    int glowLayers,
    float glowScale,
    ResourceLocation type,
    JetConeDefinition jetCone,
    BlackHoleDefinition blackHole
)
{
    public static final ResourceLocation DEFAULT_TYPE = ResourceLocation.fromNamespaceAndPath("pioneer", "main_sequence");

    private static SunDefinition fromCodec(float size, float radiusSolar, float massSolar, float axialRotationSpeed, List<Float> glowColor, int glowLayers, float glowScale, ResourceLocation type, Optional<JetConeDefinition> jetCone, Optional<BlackHoleDefinition> blackHole)
    {
        return new SunDefinition(size, radiusSolar, massSolar, axialRotationSpeed, glowColor, glowLayers, glowScale, type, jetCone.orElse(null), blackHole.orElse(null));
    }

    public static final Codec<SunDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("size").forGetter(SunDefinition::size),
            Codec.FLOAT.optionalFieldOf("radius_solar", 1.0f).forGetter(SunDefinition::radiusSolar),
            Codec.FLOAT.optionalFieldOf("mass_solar", 1.0f).forGetter(SunDefinition::massSolar),
            Codec.FLOAT.optionalFieldOf("axial_rotation_speed", 1.0f).forGetter(SunDefinition::axialRotationSpeed),
            Codec.FLOAT.listOf().optionalFieldOf("glow_color", List.of(1.0f, 0.9f, 0.6f)).forGetter(SunDefinition::glowColor),
            Codec.INT.optionalFieldOf("glow_layers", 8).forGetter(SunDefinition::glowLayers),
            Codec.FLOAT.optionalFieldOf("glow_scale", 1.8f).forGetter(SunDefinition::glowScale),
            ResourceLocation.CODEC.optionalFieldOf("type", DEFAULT_TYPE).forGetter(SunDefinition::type),
            JetConeDefinition.CODEC.optionalFieldOf("jet_cone").forGetter(sd -> Optional.ofNullable(sd.jetCone())),
            BlackHoleDefinition.CODEC.optionalFieldOf("black_hole").forGetter(sd -> Optional.ofNullable(sd.blackHole()))
        ).apply(instance, SunDefinition::fromCodec)
    );

    public float glowR() { return glowColor.size() > 0 ? glowColor.get(0) : 1f; }
    public float glowG() { return glowColor.size() > 1 ? glowColor.get(1) : 0.9f; }
    public float glowB() { return glowColor.size() > 2 ? glowColor.get(2) : 0.6f; }
}