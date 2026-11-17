package cute.ame.pioneer.Core.Readout;

public final class Readout
{
    public static final int MAX_LINES = 12;

    public static final byte NUMBER = 0;
    public static final byte TEXT = 1;
    public static final byte KEY = 2;

    private final String[] keys = new String[MAX_LINES];
    private final byte[] kinds = new byte[MAX_LINES];
    private final byte[] units = new byte[MAX_LINES];
    private final double[] values = new double[MAX_LINES];
    private final String[] texts = new String[MAX_LINES];
    private int size;

    private static double rounded(double value)
    {
        if (!Double.isFinite(value) || value == 0.0) return value;

        double scale = Math.pow(10.0, 3.0 - Math.floor(Math.log10(Math.abs(value))));
        return Math.rint(value * scale) / scale;
    }

    public void clear()
    {
        size = 0;
    }

    public int size()
    {
        return size;
    }

    public boolean number(String key, double value, ReadoutUnit unit)
    {
        return put(key, NUMBER, unit, value, "");
    }

    public boolean text(String key, String value)
    {
        return put(key, TEXT, ReadoutUnit.NONE, 0.0, value);
    }

    public boolean translated(String key, String valueKey)
    {
        return put(key, KEY, ReadoutUnit.NONE, 0.0, valueKey);
    }

    public String key(int line)
    {
        return keys[line];
    }

    public byte kind(int line)
    {
        return kinds[line];
    }

    public ReadoutUnit unit(int line)
    {
        return ReadoutUnit.byIndex(units[line]);
    }

    public double value(int line)
    {
        return values[line];
    }

    public String text(int line)
    {
        return texts[line];
    }

    public long fingerprint()
    {
        long hash = 0x9E3779B97F4A7C15L ^ size;

        for (int i = 0; i < size; i++)
        {
            hash = hash * 31L + keys[i].hashCode();
            hash = hash * 31L + (kinds[i] << 8 | units[i]);
            hash = hash * 31L + (kinds[i] == NUMBER ? Double.hashCode(rounded(values[i])) : texts[i].hashCode());
        }

        return hash;
    }

    private boolean put(String key, byte kind, ReadoutUnit unit, double value, String text)
    {
        if (size == MAX_LINES) return false;

        keys[size] = key;
        kinds[size] = kind;
        units[size] = (byte) unit.ordinal();
        values[size] = value;
        texts[size] = text;
        size++;
        return true;
    }
}
