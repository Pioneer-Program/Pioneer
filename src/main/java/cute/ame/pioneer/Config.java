package cute.ame.pioneer;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static ModConfigSpec.DoubleValue ORBIT_ENTRY_ALTITUDE_KM;
    public static ModConfigSpec.DoubleValue SHOW_OWN_PLANET_START_KM;
    public static ModConfigSpec.BooleanValue WARP_THROUGH_FRAMES;

    public static ModConfigSpec.DoubleValue BREATHING_MIN_PRESSURE_P;
    public static ModConfigSpec.DoubleValue BREATHING_MOL_PER_TICK;
    public static ModConfigSpec.DoubleValue ASPHYXIATION_DAMAGE;
    public static ModConfigSpec.IntValue ASPHYXIATION_DAMAGE_PERIOD;

    public static ModConfigSpec.DoubleValue PUMP_BOOST_P;
    public static ModConfigSpec.DoubleValue BURST_EXPLOSION_POWER;
    public static ModConfigSpec.DoubleValue BURST_EXPLOSION_MAX_POWER;

    public static ModConfigSpec.IntValue SENSOR_PERIOD_TICKS;

    public static ModConfigSpec.IntValue FRAME_GRID_RADIUS;
    public static ModConfigSpec.IntValue FRAME_CELL_SPACING;
    public static ModConfigSpec.IntValue FRAME_GRID_Y;
    public static ModConfigSpec.DoubleValue FRAME_RELEASE_MAX_SPEED;
    public static ModConfigSpec.DoubleValue FRAME_CARRY_RADIUS;
    public static ModConfigSpec.DoubleValue FRAME_JOIN_RADIUS;

    static final ModConfigSpec SPEC;
    static
    {
        BUILDER.push("Server");
        BUILDER.pop();

        BUILDER.push("Transitions");
        ORBIT_ENTRY_ALTITUDE_KM = BUILDER.defineInRange("orbit_entry_altitude_km", 100.0, 0.1, 1_000_000.0);
        SHOW_OWN_PLANET_START_KM = BUILDER.defineInRange("show_own_planet_start_km", 1.0, 0.0, 1_000_000.0);
        WARP_THROUGH_FRAMES = BUILDER.define("warp_through_frames", true);
        BUILDER.pop();

        BUILDER.push("Fluids");

        BREATHING_MIN_PRESSURE_P = BUILDER.defineInRange("breathing_min_pressure_p", 0.0618, 0.0, 10.0);
        BREATHING_MOL_PER_TICK = BUILDER.defineInRange("breathing_mol_per_tick", 0.000875, 0.0, 1.0);
        ASPHYXIATION_DAMAGE = BUILDER.defineInRange("asphyxiation_damage", 2.0, 0.0, 100.0);
        ASPHYXIATION_DAMAGE_PERIOD = BUILDER.defineInRange("asphyxiation_damage_period", 20, 1, 200);

        BURST_EXPLOSION_POWER = BUILDER.defineInRange("burst_explosion_power", 1.2, 0.0, 10.0);
        BURST_EXPLOSION_MAX_POWER = BUILDER.defineInRange("burst_explosion_max_power", 4.0, 0.0, 20.0);

        PUMP_BOOST_P = BUILDER.defineInRange("pump_boost_p", 4.0, 0.0, 100.0);

        SENSOR_PERIOD_TICKS = BUILDER.defineInRange("sensor_period_ticks", 5, 1, 200);

        BUILDER.pop();

        BUILDER.push("Frames");
        FRAME_GRID_RADIUS = BUILDER.defineInRange("frame_grid_radius", 3, 0, 8);
        FRAME_CELL_SPACING = BUILDER.defineInRange("frame_cell_spacing", 8192, 1024, 65536);
        FRAME_GRID_Y = BUILDER.defineInRange("frame_grid_y", 0, -256, 256);
        FRAME_RELEASE_MAX_SPEED = BUILDER.defineInRange("frame_release_max_speed", 100.0, 0.0, 1.0e9);
        FRAME_CARRY_RADIUS = BUILDER.defineInRange("frame_carry_radius", 16.0, 0.0, 256.0);
        FRAME_JOIN_RADIUS = BUILDER.defineInRange("frame_join_radius", 256.0, 0.0, 2048.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
