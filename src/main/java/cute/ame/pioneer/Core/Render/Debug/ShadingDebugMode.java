package cute.ame.pioneer.Core.Render.Debug;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ShadingDebugMode
{
    OFF("normal rendering"),
    SHADOW("shadow term only: green lit, red shadowed, yellow penumbra"),
    GEOMETRY("geometry: ring radius, plane hit, shadow ray length"),
    LIGHTING("illumination: mu0 (red), muV (green), optical depth (blue)"),
    PHASE("phase function (red) and opposition surge (green)"),
    TRANSFER("scattering branch: red reflection, green transmission, blue radiance"),
    PENUMBRA("penumbra width, scaled to the planet radius"),
    NORMALS("shading normal");

    private final String description;

    ShadingDebugMode(String description)
    {
        this.description = description;
    }

    public String description()
    {
        return description;
    }

    public String key()
    {
        return name().toLowerCase(Locale.ROOT);
    }

    public int shaderId()
    {
        return ordinal();
    }

    public static Optional<ShadingDebugMode> byKey(String key)
    {
        return Arrays.stream(values()).filter(m -> m.key().equalsIgnoreCase(key)).findFirst();
    }

    public static String[] keys()
    {
        return Arrays.stream(values()).map(ShadingDebugMode::key).toArray(String[]::new);
    }

    private static volatile ShadingDebugMode current = OFF;

    public static ShadingDebugMode current()
    {
        return current;
    }

    public static void set(ShadingDebugMode mode)
    {
        current = (mode == null) ? OFF : mode;
    }

    public static ShadingDebugMode cycle()
    {
        ShadingDebugMode[] all = values();
        current = all[(current.ordinal() + 1) % all.length];
        return current;
    }

    public static int currentShaderId()
    {
        return current.shaderId();
    }
}
