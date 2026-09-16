package cute.ame.pioneer.Thermal.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record ThermalDevice
(
    List<String> blocks,
    boolean cooling,
    float watts, // Joules/secs of simulation time, pass spend watts*dt, same shit as conduction
    float minSetpointK,
    float maxSetpointK,
    float defaultSetpointK
)
{
    public static final Codec<ThermalDevice> CODEC = RecordCodecBuilder.<ThermalDevice>create(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(ThermalDevice::blocks),
        Codec.BOOL.optionalFieldOf("cooling", Boolean.FALSE).forGetter(ThermalDevice::cooling),
        Codec.FLOAT.fieldOf("watts").forGetter(ThermalDevice::watts),
        Codec.FLOAT.optionalFieldOf("min_setpoint_k", 4.0f).forGetter(ThermalDevice::minSetpointK),
        Codec.FLOAT.optionalFieldOf("max_setpoint_k", 1273.15f).forGetter(ThermalDevice::maxSetpointK),
        Codec.FLOAT.optionalFieldOf("default_setpoint_k", 293.15f).forGetter(ThermalDevice::defaultSetpointK)
    ).apply(i, ThermalDevice::new)).flatXmap(ThermalDevice::validate, DataResult::success);

    public float clampSetpoint(float kelvin)
    {
        if (!Float.isFinite(kelvin)) return defaultSetpointK;
        if (kelvin < minSetpointK) return minSetpointK;
        if (kelvin > maxSetpointK) return maxSetpointK;

        return kelvin;
    }

    private static DataResult<ThermalDevice> validate(ThermalDevice d)
    {
        if (!Float.isFinite(d.watts) || d.watts <= 0.0f)
            return DataResult.error(() -> "watts must be finite and > 0, got " + d.watts);

        if (!(d.minSetpointK > 0.0f) || !(d.maxSetpointK > d.minSetpointK))
            return DataResult.error(() -> "expected 0 < min_setpoint_k < max_setpoint_k, got " + d.minSetpointK + " and " + d.maxSetpointK);

        if (d.defaultSetpointK < d.minSetpointK || d.defaultSetpointK > d.maxSetpointK)
            return DataResult.error(() -> "default_setpoint_k must sit within the setpoint range, got " + d.defaultSetpointK);

        return DataResult.success(d);
    }
}
