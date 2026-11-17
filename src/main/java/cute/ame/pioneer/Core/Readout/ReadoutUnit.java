package cute.ame.pioneer.Core.Readout;

import java.util.Locale;

public enum ReadoutUnit
{
    NONE("%.2f", ""),
    PRESSURE("%.3f", " P"),
    CELSIUS("%.1f", " °C"),
    KELVIN("%.1f", " K"),
    MOLE("%.3f", " mol"),
    PERCENT("%.1f", " %"),
    WATT("%.0f", " W"),
    LITRE("%.0f", " L");

    private static final ReadoutUnit[] VALUES = values();

    private final String pattern;
    private final String suffix;

    ReadoutUnit(String pattern, String suffix)
    {
        this.pattern = pattern;
        this.suffix = suffix;
    }

    public static ReadoutUnit byIndex(int index)
    {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }

    public String number(double value)
    {
        return String.format(Locale.ROOT, pattern, value);
    }

    public String suffix()
    {
        return suffix;
    }

    public String format(double value)
    {
        return number(value) + suffix;
    }
}
