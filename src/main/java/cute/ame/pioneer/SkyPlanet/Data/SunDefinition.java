package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Physics.Blackbody;
import cute.ame.pioneer.SkyPlanet.Physics.StellarPhysics;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SunDefinition(
    float size,
    float massSolar,
    StellarPhysics.Stage stage,
    float axialRotationSpeed,
    Optional<Float> temperatureOverrideK,
    JetConeDefinition jetCone,
    BlackHoleDefinition blackHole
)
{
    public static final Codec<StellarPhysics.Stage> STAGE_CODEC =
        Codec.STRING.xmap(StellarPhysics.Stage::byKey, StellarPhysics.Stage::key);

    private static SunDefinition fromCodec(float size, float massSolar, StellarPhysics.Stage stage, float axialRotationSpeed, Optional<Float> temperatureOverrideK, Optional<JetConeDefinition> jetCone, Optional<BlackHoleDefinition> blackHole)
    {
        return new SunDefinition(size, massSolar, stage, axialRotationSpeed, temperatureOverrideK, jetCone.orElse(null), blackHole.orElse(null));
    }

    public static final Codec<SunDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.FLOAT.fieldOf("size").forGetter(SunDefinition::size),
            Codec.FLOAT.optionalFieldOf("mass_solar", 1.0f).forGetter(SunDefinition::massSolar),
            STAGE_CODEC.optionalFieldOf("stage", StellarPhysics.Stage.MAIN_SEQUENCE).forGetter(SunDefinition::stage),
            Codec.FLOAT.optionalFieldOf("axial_rotation_speed", 1.0f).forGetter(SunDefinition::axialRotationSpeed),
            Codec.FLOAT.optionalFieldOf("temperature_k").forGetter(SunDefinition::temperatureOverrideK),
            JetConeDefinition.CODEC.optionalFieldOf("jet_cone").forGetter(sd -> Optional.ofNullable(sd.jetCone())),
            BlackHoleDefinition.CODEC.optionalFieldOf("black_hole").forGetter(sd -> Optional.ofNullable(sd.blackHole()))
        ).apply(instance, SunDefinition::fromCodec)
    );

    public float radiusSolar()
    {
        return (float) StellarPhysics.radiusSolar(massSolar, stage);
    }

    public float luminositySolar()
    {
        if (StellarPhysics.isCoolingRemnant(stage)) return (float) StellarPhysics.luminosityFromRadiusAndTemp(radiusSolar(), temperatureK());

        return (float) StellarPhysics.luminositySolar(massSolar, stage);
    }
    public float temperatureK()
    {
        if (StellarPhysics.isCoolingRemnant(stage) && temperatureOverrideK.isPresent()) return Math.max(temperatureOverrideK.get(), 1.0f);
        return (float) StellarPhysics.temperatureK(massSolar, stage);
    }
    public char spectralClass()
    {
        return StellarPhysics.spectralClass(temperatureK());
    }

    public float whiteHot()
    {
        return StellarPhysics.whiteHotFraction(temperatureK());
    }

    public float glowScale()
    {
        return StellarPhysics.glowScale(massSolar, stage);
    }

    private float[] rgb()
    {
        if (stage == StellarPhysics.Stage.BLACK_HOLE) return new float[] { 0.0f, 0.0f, 0.0f };
        return Blackbody.linearRgb(temperatureK());
    }

    public float glowR() { return rgb()[0]; }
    public float glowG() { return rgb()[1]; }
    public float glowB() { return rgb()[2]; }

    public JetConeDefinition effectiveJetCone()
    {
        return StellarPhysics.supportsJet(stage) ? jetCone : null;
    }

    public ResourceLocation type()
    {
        return switch (stage)
        {
            case GIANT -> ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "red_giant");
            case WHITE_DWARF -> ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "white_dwarf");
            case NEUTRON_STAR -> ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "neutron_star");
            case BLACK_HOLE -> ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "black_hole");
            default -> ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "main_sequence");
        };
    }
}
