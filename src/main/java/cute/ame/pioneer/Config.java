package cute.ame.pioneer;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static ModConfigSpec.DoubleValue ORBIT_ENTRY_ALTITUDE_KM;
    public static ModConfigSpec.DoubleValue SHOW_OWN_PLANET_START_KM;

    static final ModConfigSpec SPEC;
    static
    {
        BUILDER.push("Server");
        BUILDER.pop();

        BUILDER.push("Transitions");
        ORBIT_ENTRY_ALTITUDE_KM = BUILDER.defineInRange("orbit_entry_altitude_km", 100.0, 0.1, 1_000_000.0);
        SHOW_OWN_PLANET_START_KM = BUILDER.defineInRange("show_own_planet_start_km", 5.0, 0.0, 1_000_000.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}