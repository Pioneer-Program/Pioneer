package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RingDefinition(
    float innerRadius,
    float outerRadius,
    List<Float> colorMin,
    List<Float> colorMax,
    int ringCount,
    float opacity,
    long seed
)
{
    public static final Codec<RingDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("inner_radius").forGetter(RingDefinition::innerRadius),
            Codec.FLOAT.fieldOf("outer_radius").forGetter(RingDefinition::outerRadius),
            Codec.FLOAT.listOf().optionalFieldOf("color_min", List.of(0.3f,0.3f,0.3f)).forGetter(RingDefinition::colorMin),
            Codec.FLOAT.listOf().optionalFieldOf("color_max", List.of(0.7f,0.7f,0.7f)).forGetter(RingDefinition::colorMax),
            Codec.INT.optionalFieldOf("ring_count", 5).forGetter(RingDefinition::ringCount),
            Codec.FLOAT.optionalFieldOf("opacity", 0.6f).forGetter(RingDefinition::opacity),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(RingDefinition::seed)
        ).apply(instance, RingDefinition::new)
    );

    public float minR() { return colorMin.size()>=3 ? colorMin.get(0) : 0.3f; }
    public float minG() { return colorMin.size()>=3 ? colorMin.get(1) : 0.3f; }
    public float minB() { return colorMin.size()>=3 ? colorMin.get(2) : 0.3f; }
    public float maxR() { return colorMax.size()>=3 ? colorMax.get(0) : 0.7f; }
    public float maxG() { return colorMax.size()>=3 ? colorMax.get(1) : 0.7f; }
    public float maxB() { return colorMax.size()>=3 ? colorMax.get(2) : 0.7f; }
}