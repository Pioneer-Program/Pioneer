package cute.ame.pioneer.SkyPlanet.Physics;

import java.util.Locale;

public final class StellarPhysics
{
    public enum Stage
    {
        MAIN_SEQUENCE,
        GIANT,
        WHITE_DWARF,
        NEUTRON_STAR,
        BLACK_HOLE;

        public String key() { return name().toLowerCase(Locale.ROOT); }

        public static Stage byKey(String key)
        {
            for (Stage s : values()) if (s.key().equalsIgnoreCase(key)) return s;
            return MAIN_SEQUENCE;
        }
    }

    public static final double SUN_TEFF_K = 5772.0;
    public static final double DEFAULT_WHITE_DWARF_K = 15000.0;
    public static final double DEFAULT_NEUTRON_STAR_K = 600000.0;

    public static boolean isCoolingRemnant(Stage stage)
    {
        return stage == Stage.WHITE_DWARF || stage == Stage.NEUTRON_STAR;
    }

    public static double defaultTemperatureK(Stage stage)
    {
        return switch (stage)
        {
            case WHITE_DWARF -> DEFAULT_WHITE_DWARF_K;
            case NEUTRON_STAR -> DEFAULT_NEUTRON_STAR_K;
            default -> 0.0;
        };
    }

    public static double mainSequenceRadius(double massSolar)
    {
        double m = Math.max(massSolar, 1e-3);
        if (m < 1.0) return Math.pow(m, 0.80);
        if (m < 10.0) return Math.pow(m, 0.75);
        return 1.777 * Math.sqrt(m);
    }

    public static double luminositySolar(double massSolar, Stage stage)
    {
        double m = Math.max(massSolar, 1e-3);
        double l;
        if (m < 0.43) l = 0.23 * Math.pow(m, 2.3);
        else if (m < 2.0) l = Math.pow(m, 4.0);
        else if (m < 20.0) l = 1.4 * Math.pow(m, 3.5);
        else l = 1.4 * Math.pow(20.0, 3.5) * Math.pow(m / 20.0, 2.6);

        return switch (stage)
        {
            case GIANT -> l * 100.0;
            case WHITE_DWARF, NEUTRON_STAR -> luminosityFromRadiusAndTemp(radiusSolar(massSolar, stage), defaultTemperatureK(stage));
            case BLACK_HOLE -> 0.0;
            default -> l;
        };
    }

    public static double radiusSolar(double massSolar, Stage stage)
    {
        return switch (stage)
        {
            case GIANT -> mainSequenceRadius(massSolar) * 100.0;
            case WHITE_DWARF -> 0.0127;
            case NEUTRON_STAR -> 1.44e-5;
            case BLACK_HOLE -> schwarzschildRadiusSolar(massSolar);
            default -> mainSequenceRadius(massSolar);
        };
    }

    public static double luminosityFromRadiusAndTemp(double radiusSolar, double temperatureK)
    {
        double r = Math.max(radiusSolar, 1e-12);
        double t = Math.max(temperatureK, 1.0) / SUN_TEFF_K;
        return r * r * t * t * t * t;
    }

    public static double schwarzschildRadiusSolar(double massSolar)
    {
        return 2.953e3 * Math.max(massSolar, 1e-3) / 6.957e8;
    }

    public static double temperatureK(double massSolar, Stage stage)
    {
        if (stage == Stage.BLACK_HOLE) return 0.0;
        if (isCoolingRemnant(stage)) return defaultTemperatureK(stage);

        double l = luminositySolar(massSolar, stage);
        double r = radiusSolar(massSolar, stage);
        if (r <= 1e-12) return 0.0;

        return SUN_TEFF_K * Math.pow(l / (r * r), 0.25);
    }

    public static char spectralClass(double temperatureK)
    {
        if (temperatureK >= 30000.0) return 'O';
        if (temperatureK >= 10000.0) return 'B';
        if (temperatureK >= 7500.0) return 'A';
        if (temperatureK >= 6000.0) return 'F';
        if (temperatureK >= 5200.0) return 'G';
        if (temperatureK >= 3700.0) return 'K';
        return 'M';
    }

    public static float[] colorRgb(double massSolar, Stage stage)
    {
        if (stage == Stage.BLACK_HOLE) return new float[] { 0.0f, 0.0f, 0.0f };
        return Blackbody.linearRgb(temperatureK(massSolar, stage));
    }

    public static boolean supportsJet(Stage stage)
    {
        return stage == Stage.NEUTRON_STAR || stage == Stage.BLACK_HOLE;
    }

    public static float whiteHotFraction(double temperatureK)
    {
        double ratio = Math.max(temperatureK, 1.0) / SUN_TEFF_K;
        double surfaceBrightness = ratio * ratio * ratio * ratio;
        return (float) Math.clamp(1.0 - Math.exp(-0.7 * surfaceBrightness), 0.0, 1.0);
    }

    public static float glowScale(double massSolar, Stage stage)
    {
        double l = Math.max(luminositySolar(massSolar, stage), 1e-6);
        return (float) Math.clamp(2.6 * Math.pow(l, 0.03), 1.35, 3.0);
    }

    public static double irradianceRelative(double luminositySolar, double distanceAu)
    {
        double d = Math.max(distanceAu, 1e-6);
        return luminositySolar / (d * d);
    }
}
