package cute.ame.pioneer;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static ModConfigSpec.DoubleValue ORBIT_ENTRY_ALTITUDE_KM;
    public static ModConfigSpec.DoubleValue SHOW_OWN_PLANET_START_KM;

    public static ModConfigSpec.DoubleValue ROOM_LITRES_PER_BLOCK;
    public static ModConfigSpec.IntValue ROOM_MAX_BLOCKS;
    public static ModConfigSpec.IntValue ROOM_RESCANS_PER_TICK;

    public static ModConfigSpec.DoubleValue AMBIENT_EQUALIZE_RATE;
    public static ModConfigSpec.DoubleValue AMBIENT_EQUALIZE_EPSILON;

    public static ModConfigSpec.DoubleValue BREATHING_MIN_PRESSURE_P;
    public static ModConfigSpec.DoubleValue BREATHING_MOL_PER_TICK;
    public static ModConfigSpec.DoubleValue ASPHYXIATION_DAMAGE;
    public static ModConfigSpec.IntValue ASPHYXIATION_DAMAGE_PERIOD;

    public static ModConfigSpec.IntValue VESSEL_MAX_CLUSTER;

    public static ModConfigSpec.IntValue FLUID_SWEEPS_PER_TICK;

    public static ModConfigSpec.DoubleValue FLUID_SLEEP_EPSILON;
    public static ModConfigSpec.IntValue FLUID_SLEEP_TICKS;
    public static ModConfigSpec.DoubleValue FLUID_SLEEP_TEMPERATURE_EPSILON;
    public static ModConfigSpec.DoubleValue FLUID_THERMAL_CONDUCTANCE;

    public static ModConfigSpec.BooleanValue BURST_ENABLED;
    public static ModConfigSpec.DoubleValue BURST_JITTER;
    public static ModConfigSpec.DoubleValue BURST_SCREEN_PRESSURE;

    public static ModConfigSpec.DoubleValue PUMP_BOOST_P;
    public static ModConfigSpec.DoubleValue BURST_EXPLOSION_POWER;
    public static ModConfigSpec.DoubleValue BURST_EXPLOSION_MAX_POWER;

    public static ModConfigSpec.IntValue SENSOR_PERIOD_TICKS;

    public static ModConfigSpec.DoubleValue FLUID_BLOCK_COUPLING;
    public static ModConfigSpec.IntValue FLUID_COUPLING_PERIOD;
    public static ModConfigSpec.IntValue FLUID_COUPLING_SAMPLES;
    public static ModConfigSpec.DoubleValue VACUUM_BLOCK_TEMPERATURE_K;

    static final ModConfigSpec SPEC;
    static
    {
        BUILDER.push("Server");
        BUILDER.pop();

        BUILDER.push("Transitions");
        ORBIT_ENTRY_ALTITUDE_KM = BUILDER.defineInRange("orbit_entry_altitude_km", 100.0, 0.1, 1_000_000.0);
        SHOW_OWN_PLANET_START_KM = BUILDER.defineInRange("show_own_planet_start_km", 1.0, 0.0, 1_000_000.0);
        BUILDER.pop();

        BUILDER.push("Fluids");
        ROOM_LITRES_PER_BLOCK = BUILDER.defineInRange("room_litres_per_block", 200.0, 1.0, 1000.0);
        ROOM_MAX_BLOCKS = BUILDER.defineInRange("room_max_blocks", 4096, 64, 65_536);
        ROOM_RESCANS_PER_TICK = BUILDER.defineInRange("room_rescans_per_tick", 1, 1, 16);

        AMBIENT_EQUALIZE_RATE = BUILDER.defineInRange("ambient_equalize_rate", 0.25, 0.001, 1.0);
        AMBIENT_EQUALIZE_EPSILON = BUILDER.defineInRange("ambient_equalize_epsilon", 1.0e-4, 0.0, 1.0);

        BREATHING_MIN_PRESSURE_P = BUILDER.defineInRange("breathing_min_pressure_p", 0.0618, 0.0, 10.0);
        BREATHING_MOL_PER_TICK = BUILDER.defineInRange("breathing_mol_per_tick", 0.000875, 0.0, 1.0);
        ASPHYXIATION_DAMAGE = BUILDER.defineInRange("asphyxiation_damage", 2.0, 0.0, 100.0);
        ASPHYXIATION_DAMAGE_PERIOD = BUILDER.defineInRange("asphyxiation_damage_period", 20, 1, 200);

        VESSEL_MAX_CLUSTER = BUILDER.defineInRange("vessel_max_cluster", 4096, 8, 65_536);
        FLUID_SWEEPS_PER_TICK = BUILDER.defineInRange("fluid_sweeps_per_tick", 1, 1, 8);

        FLUID_SLEEP_EPSILON = BUILDER.defineInRange("fluid_sleep_epsilon", 1.0e-5, 0.0, 1.0);
        FLUID_SLEEP_TICKS = BUILDER.defineInRange("fluid_sleep_ticks", 20, 1, 1200);
        FLUID_SLEEP_TEMPERATURE_EPSILON = BUILDER.defineInRange("fluid_sleep_temperature_epsilon", 0.01, 0.0, 100.0);
        FLUID_THERMAL_CONDUCTANCE = BUILDER.defineInRange("fluid_thermal_conductance", 0.05, 0.0, 1.0);

        BURST_ENABLED = BUILDER.define("burst_enabled", true);
        BURST_JITTER = BUILDER.defineInRange("burst_jitter", 0.15, 0.0, 0.9);
        BURST_SCREEN_PRESSURE = BUILDER.defineInRange("burst_screen_pressure", 8.0, 0.1, 1000.0);
        BURST_EXPLOSION_POWER = BUILDER.defineInRange("burst_explosion_power", 1.2, 0.0, 10.0);
        BURST_EXPLOSION_MAX_POWER = BUILDER.defineInRange("burst_explosion_max_power", 4.0, 0.0, 20.0);

        PUMP_BOOST_P = BUILDER.defineInRange("pump_boost_p", 4.0, 0.0, 100.0);

        SENSOR_PERIOD_TICKS = BUILDER.defineInRange("sensor_period_ticks", 5, 1, 200);

        FLUID_BLOCK_COUPLING = BUILDER.defineInRange("fluid_block_coupling", 0.001, 0.0, 1.0);
        FLUID_COUPLING_PERIOD = BUILDER.defineInRange("fluid_coupling_period", 20, 1, 200);
        FLUID_COUPLING_SAMPLES = BUILDER.defineInRange("fluid_coupling_samples", 4, 1, 64);
        VACUUM_BLOCK_TEMPERATURE_K = BUILDER.defineInRange("vacuum_block_temperature_k", 293.15, 0.1, 1000.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
