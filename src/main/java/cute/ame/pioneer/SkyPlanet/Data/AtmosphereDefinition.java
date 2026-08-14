package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.pioneer.SkyPlanet.Physics.AtmosphericPhysics;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AtmosphereDefinition
(
    Optional<Boolean> breathable,
    Map<String, Float> composition,
    float surfacePressureBar,
    List<Float> hazeColor,
    float hazeOpticalDepth,
    float hazeAsymmetry,
    float greenhouseFraction,
    float atmosphereExaggeration
)
{
    public static final Map<String, Float> EARTH_LIKE = Map.of("n2", 0.78f, "o2", 0.21f, "ar", 0.01f);

    public static final Codec<AtmosphereDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.optionalFieldOf("breathable").forGetter(AtmosphereDefinition::breathable),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("composition", EARTH_LIKE).forGetter(AtmosphereDefinition::composition),
            Codec.FLOAT.optionalFieldOf("surface_pressure_bar", 1.0f).forGetter(AtmosphereDefinition::surfacePressureBar),
            Codec.FLOAT.listOf().optionalFieldOf("haze_color", List.of(0.85f, 0.75f, 0.6f)).forGetter(AtmosphereDefinition::hazeColor),
            Codec.FLOAT.optionalFieldOf("haze_optical_depth", 0.0f).forGetter(AtmosphereDefinition::hazeOpticalDepth),
            Codec.FLOAT.optionalFieldOf("haze_asymmetry", 0.76f).forGetter(AtmosphereDefinition::hazeAsymmetry),
            Codec.FLOAT.optionalFieldOf("greenhouse_fraction", 0.0f).forGetter(AtmosphereDefinition::greenhouseFraction),
            Codec.FLOAT.optionalFieldOf("atmosphere_exaggeration", 12.0f).forGetter(AtmosphereDefinition::atmosphereExaggeration)
        ).apply(instance, AtmosphereDefinition::new)
    );

    public double meanMolarMass()
    {
        return AtmosphericPhysics.meanMolarMass(composition);
    }

    public double[] rayleighDepth(double gravityMs2)
    {
        return AtmosphericPhysics.rayleighOpticalDepth(composition, surfacePressureBar, gravityMs2);
    }

    public float[] colorRgb(double gravityMs2)
    {
        return AtmosphericPhysics.skyColorRgb(composition, surfacePressureBar, gravityMs2, hazeRgb(), hazeOpticalDepth);
    }

    public float opacity(double gravityMs2)
    {
        return AtmosphericPhysics.opacityFromDepth(rayleighDepth(gravityMs2)[1], hazeOpticalDepth);
    }

    public float mieG()
    {
        return hazeAsymmetry;
    }

    public float mieStrength()
    {
        return Math.min(2.0f, 0.5f + 4.0f * Math.max(hazeOpticalDepth, 0.0f));
    }

    public float ozoneStrength()
    {
        return (float) Math.min(1.5, AtmosphericPhysics.ozoneFraction(composition) / 0.21);
    }

    public float rayleighScaleHeightFrac(double temperatureK, double gravityMs2, double radiusKm)
    {
        return 0.35f;
    }

    public float mieScaleHeightFrac(double temperatureK, double gravityMs2, double radiusKm)
    {
        return 0.12f;
    }

    public float shellScale(double temperatureK, double gravityMs2, double radiusKm)
    {
        double h = AtmosphericPhysics.scaleHeightMetres(meanMolarMass(), temperatureK, gravityMs2);
        double frac = h / (Math.max(radiusKm, 1e-3) * 1000.0);
        return 1.0f + (float) Math.min(0.6, frac * 12.0 * atmosphereExaggeration());
    }

    public float[] hazeRgb()
    {
        return new float[] { comp(hazeColor, 0, 0.85f), comp(hazeColor, 1, 0.75f), comp(hazeColor, 2, 0.6f) };
    }

    private static float comp(List<Float> list, int idx, float fallback)
    {
        return (list != null && list.size() > idx) ? list.get(idx) : fallback;
    }

    public float atmosphereExaggeration()
    {
        return atmosphereExaggeration;
    }
}
