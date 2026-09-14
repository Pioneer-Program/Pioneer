package cute.ame.pioneer.Thermal.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record ThermalMaterial
(
    List<String> blocks,
    float conductivity,
    float volumetricHeat,
    Optional<Float> breakdownK,
    Optional<String> breakdownResult,
    float emissivity,
    int priority
)
{
    public static final float NEVER_BREAKS = Float.MAX_VALUE;
    public static final ThermalMaterial FALLBACK = new ThermalMaterial(List.of(), 1.0f, 2.0e6f, Optional.empty(), Optional.empty(), 0.90f, Integer.MIN_VALUE);

    public static final Codec<ThermalMaterial> CODEC = RecordCodecBuilder.<ThermalMaterial>create(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(ThermalMaterial::blocks),
        Codec.FLOAT.fieldOf("conductivity").forGetter(ThermalMaterial::conductivity),
        Codec.FLOAT.fieldOf("volumetric_heat").forGetter(ThermalMaterial::volumetricHeat),
        Codec.FLOAT.optionalFieldOf("breakdown_k").forGetter(ThermalMaterial::breakdownK),
        Codec.STRING.optionalFieldOf("breakdown_result").forGetter(ThermalMaterial::breakdownResult),
        Codec.FLOAT.optionalFieldOf("emissivity", 0.90f).forGetter(ThermalMaterial::emissivity),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(ThermalMaterial::priority)
    ).apply(i, ThermalMaterial::new)).flatXmap(ThermalMaterial::validate, DataResult::success);

    public float breakdown()
    {
        return breakdownK.orElse(NEVER_BREAKS);
    }

    private static DataResult<ThermalMaterial> validate(ThermalMaterial m)
    {
        if (!Float.isFinite(m.conductivity) || m.conductivity < 0.0f)
            return DataResult.error(() -> "conductivity must be finite and >= 0, got " + m.conductivity);

        if (!Float.isFinite(m.volumetricHeat) || m.volumetricHeat <= 0.0f)
            return DataResult.error(() -> "volumetric_heat must be finite and > 0, got " + m.volumetricHeat);

        if (m.emissivity < 0.0f || m.emissivity > 1.0f)
            return DataResult.error(() -> "emissivity must be within [0, 1], got " + m.emissivity);

        if (m.breakdownK.isPresent() && !(m.breakdownK.get() > 0.0f))
            return DataResult.error(() -> "breakdown_k must be > 0, got " + m.breakdownK.get());

        return DataResult.success(m);
    }
}
