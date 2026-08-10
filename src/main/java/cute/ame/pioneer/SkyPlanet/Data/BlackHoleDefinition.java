package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record BlackHoleDefinition(
    List<Float> color,
    float scale,
    float size,
    int steps,
    float speed,
    float intensity,
    float diskTilt
)
{
    public static final Codec<BlackHoleDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.listOf().optionalFieldOf("color", List.of(-1.0f, -1.0f, -1.0f)).forGetter(BlackHoleDefinition::color),
            Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(BlackHoleDefinition::scale),
            Codec.FLOAT.optionalFieldOf("size", 0.25f).forGetter(BlackHoleDefinition::size),
            Codec.INT.optionalFieldOf("steps", 12).forGetter(BlackHoleDefinition::steps),
            Codec.FLOAT.optionalFieldOf("speed", 1.0f).forGetter(BlackHoleDefinition::speed),
            Codec.FLOAT.optionalFieldOf("intensity", 200.0f).forGetter(BlackHoleDefinition::intensity),
            Codec.FLOAT.optionalFieldOf("diskTilt", 30.0f).forGetter(BlackHoleDefinition::diskTilt)
        ).apply(instance, BlackHoleDefinition::new)
    );

    public float r() { return color.size() > 0 ? color.get(0) : -1f; }
    public float g() { return color.size() > 1 ? color.get(1) : -1f; }
    public float b() { return color.size() > 2 ? color.get(2) : -1f; }
}
