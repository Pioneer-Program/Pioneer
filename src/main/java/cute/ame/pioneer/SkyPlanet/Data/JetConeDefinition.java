package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record JetConeDefinition(
    float length,
    float baseRadius,
    float tipRadius,
    List<Float> color,
    float intensity,
    float axialTilt,
    float precessionSpeed,
    float wobbleSpeed,
    float helixTurns,
    float fiberDistortion,
    float trailPersistence,
    int segments
)
{
    public static final Codec<JetConeDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("length").forGetter(JetConeDefinition::length),
            Codec.FLOAT.optionalFieldOf("base_radius", 0.15f).forGetter(JetConeDefinition::baseRadius),
            Codec.FLOAT.optionalFieldOf("tip_radius", 0.55f).forGetter(JetConeDefinition::tipRadius),
            Codec.FLOAT.listOf().optionalFieldOf("color", List.of(0.6f, 0.85f, 1.0f)).forGetter(JetConeDefinition::color),
            Codec.FLOAT.optionalFieldOf("intensity", 1.0f).forGetter(JetConeDefinition::intensity),
            Codec.FLOAT.optionalFieldOf("axial_tilt", 15.0f).forGetter(JetConeDefinition::axialTilt),
            Codec.FLOAT.optionalFieldOf("precession_speed", 0.15f).forGetter(JetConeDefinition::precessionSpeed),
            Codec.FLOAT.optionalFieldOf("wobble_speed", 3.0f).forGetter(JetConeDefinition::wobbleSpeed),
            Codec.FLOAT.optionalFieldOf("helix_turns", 2.2f).forGetter(JetConeDefinition::helixTurns),
            Codec.FLOAT.optionalFieldOf("fiber_distortion", 0.55f).forGetter(JetConeDefinition::fiberDistortion),
            Codec.FLOAT.optionalFieldOf("trail_persistence", 0.45f).forGetter(JetConeDefinition::trailPersistence),
            Codec.INT.optionalFieldOf("segments", 14).forGetter(JetConeDefinition::segments)
        ).apply(instance, JetConeDefinition::new)
    );

    public float r() { return color.size() > 0 ? color.get(0) : 0.6f; }
    public float g() { return color.size() > 1 ? color.get(1) : 0.85f; }
    public float b() { return color.size() > 2 ? color.get(2) : 1.0f; }
}