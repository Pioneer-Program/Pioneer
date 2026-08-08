package cute.ame.auralithpioneerinitiative;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static ModConfigSpec.IntValue ORBIT_ENTRY_Y;
    public static ModConfigSpec.IntValue SHOW_OWN_PLANET_START_Y;
    public static ModConfigSpec.IntValue SURFACE_PATCH_WORLD_RADIUS;
    public static ModConfigSpec.IntValue SURFACE_PATCH_SEA_LEVEL;
    public static ModConfigSpec.IntValue SELF_TILT_START_Y;
    public static ModConfigSpec.IntValue SELF_TILT_END_OFFSET;

    static final ModConfigSpec SPEC;
    static
    {
        BUILDER.push("Server");
        BUILDER.pop();

        BUILDER.push("Transitions");
        ORBIT_ENTRY_Y = BUILDER
                .comment("Y level above which a player in a surface dimension is sent to its orbit dimension.")
                .defineInRange("orbit_entry_y", 1000, 64, 100_000);
        SHOW_OWN_PLANET_START_Y = BUILDER
                .comment("Y level above which a player standing on a planet's surface begins to see that planet's own cubemap fade into their sky, reaching full visibility at orbit_entry_y.")
                .defineInRange("show_own_planet_start_y", 80, 0, 100_000);
        SURFACE_PATCH_WORLD_RADIUS = BUILDER
                .comment("Purely a visual scale factor, NOT a real planet radius: how many blocks (from the preload anchor) map across the full [-0.5,0.5] extent of one cube face when drawing the real-chunk surface patch. Smaller = the patch looks bigger relative to the cube.")
                .defineInRange("surface_patch_world_radius", 400, 16, 100_000);
        SURFACE_PATCH_SEA_LEVEL = BUILDER
                .comment("Y level (+1) used as the flat reference plane for the surface patch. Deliberately a fixed constant instead of sampling the heightmap at the anchor column -- sampling per-anchor made the reference height jump around with local terrain (hills/valleys right at the anchor), which is what made the patch look like it was floating/misaligned relative to the cube surface. Set to match this world's actual sea level.")
                .defineInRange("surface_patch_sea_level", 63, -2032, 2032);
        SELF_TILT_START_Y = BUILDER
                .comment("Y level above which the self planet's own cubemap body begins to progressively rotate into its true axial tilt + spin. Below this Y the body is shown untilted (identity orientation) so the player standing right on the surface isn't looking at a body rotated so far that its faces get backface-culled from directly underneath.")
                .defineInRange("self_tilt_start_y", 256, 0, 100_000);
        SELF_TILT_END_OFFSET = BUILDER
                .comment("Number of Y levels below orbit_entry_y (the dimension swap height) at which the self planet's tilt/spin reaches its full, true value. E.g. with orbit_entry_y=1000 and this set to 50, full tilt is reached at Y=950, so the last stretch before the swap already shows the exact orientation the space dimension will hand off to, making the swap seamless.")
                .defineInRange("self_tilt_end_offset", 50, 0, 100_000);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}