package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GasDefinition
(
    double molarMassGPerMol,
    double refractivity1e8,
    boolean producesOzone,
    boolean breathable,
    float requiredFraction,
    float hazardFraction,
    float greenhousePotency,
    float specificHeat,
    float boilingPointK,
    float latentHeatJPerKg
)
{
    public static final GasDefinition FALLBACK = new GasDefinition(28.97, 29000.0, false, true, 0.0f, 1.0f, 0.0f, 1005.0f, 78.9f, 199_000.0f);

    public static final Codec<GasDefinition> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.DOUBLE.fieldOf("molar_mass").forGetter(GasDefinition::molarMassGPerMol),
            Codec.DOUBLE.optionalFieldOf("refractivity", 29000.0).forGetter(GasDefinition::refractivity1e8),
            Codec.BOOL.optionalFieldOf("produces_ozone", false).forGetter(GasDefinition::producesOzone),
            Codec.BOOL.optionalFieldOf("breathable", false).forGetter(GasDefinition::breathable),
            Codec.FLOAT.optionalFieldOf("required_fraction", 0.0f).forGetter(GasDefinition::requiredFraction),
            Codec.FLOAT.optionalFieldOf("hazard_fraction", 1.0f).forGetter(GasDefinition::hazardFraction),
            Codec.FLOAT.optionalFieldOf("greenhouse_potency", 0.0f).forGetter(GasDefinition::greenhousePotency),
            Codec.FLOAT.optionalFieldOf("specific_heat", 1005.0f).forGetter(GasDefinition::specificHeat),
            Codec.FLOAT.optionalFieldOf("boiling_point", 78.9f).forGetter(GasDefinition::boilingPointK),
            Codec.FLOAT.optionalFieldOf("latent_heat", 199_000.0f).forGetter(GasDefinition::latentHeatJPerKg)
        ).apply(i, GasDefinition::new)
    );
}
