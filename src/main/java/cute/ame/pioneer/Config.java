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

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}