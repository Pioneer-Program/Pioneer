package cute.ame.pioneer.Gas.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GasProperties
(
    double refractivity1e8,
    boolean producesOzone,
    float greenhousePotency,
    boolean breathable,
    float requiredFraction,
    float hazardFraction
)
{
    public static final GasProperties FALLBACK = new GasProperties(29000.0, false, 0.0f, true, 0.0f, 1.0f);

    public static final Codec<GasProperties> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.DOUBLE.optionalFieldOf("refractivity", 29000.0).forGetter(GasProperties::refractivity1e8),
            Codec.BOOL.optionalFieldOf("produces_ozone", false).forGetter(GasProperties::producesOzone),
            Codec.FLOAT.optionalFieldOf("greenhouse_potency", 0.0f).forGetter(GasProperties::greenhousePotency),
            Codec.BOOL.optionalFieldOf("breathable", false).forGetter(GasProperties::breathable),
            Codec.FLOAT.optionalFieldOf("required_fraction", 0.0f).forGetter(GasProperties::requiredFraction),
            Codec.FLOAT.optionalFieldOf("hazard_fraction", 1.0f).forGetter(GasProperties::hazardFraction)
        ).apply(i, GasProperties::new)
    );
}
