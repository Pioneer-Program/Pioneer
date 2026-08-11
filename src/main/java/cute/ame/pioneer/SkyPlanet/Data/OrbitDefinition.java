package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record OrbitDefinition
        (
    double radius,
    double periodDays,
    double startAngle,
    double inclination,
    double eccentricity,
    double ascendingNode,
    double argPeriapsis
)
{
    public static final Codec<OrbitDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("radius").forGetter(OrbitDefinition::radius),
            Codec.DOUBLE.fieldOf("period_days").forGetter(OrbitDefinition::periodDays),
            Codec.DOUBLE.optionalFieldOf("start_angle", 0.0).forGetter(OrbitDefinition::startAngle),
            Codec.DOUBLE.optionalFieldOf("inclination", 0.0).forGetter(OrbitDefinition::inclination),
            Codec.DOUBLE.optionalFieldOf("eccentricity", 0.0).forGetter(OrbitDefinition::eccentricity),
            Codec.DOUBLE.optionalFieldOf("ascending_node", 0.0).forGetter(OrbitDefinition::ascendingNode),
            Codec.DOUBLE.optionalFieldOf("arg_periapsis", 0.0).forGetter(OrbitDefinition::argPeriapsis)
        ).apply(instance, OrbitDefinition::new)
    );
}