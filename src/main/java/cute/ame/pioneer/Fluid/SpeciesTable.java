package cute.ame.pioneer.Fluid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpeciesTable
{
    public static final int UNKNOWN = -1;

    public static final SpeciesTable EMPTY = new SpeciesTable(new String[0], new float[0], new float[0]);

    private final String[] keys;
    private final Map<String, Integer> index;

    private final float[] molarMass;
    private final float[] specificHeat;

    private final boolean[] breathable;
    private final float[] requiredPressure;
    private final float[] hazardPressure;

    public SpeciesTable(String[] keys, float[] molarMass, float[] specificHeat)
    {
        this(keys, molarMass, specificHeat, new boolean[keys.length], filled(keys.length, 0.0f), filled(keys.length, Float.MAX_VALUE));
    }

    public SpeciesTable(String[] keys, float[] molarMass, float[] specificHeat, boolean[] breathable, float[] requiredPressure, float[] hazardPressure)
    {
        this.keys = keys;
        this.molarMass = molarMass;
        this.specificHeat = specificHeat;
        this.breathable = breathable;
        this.requiredPressure = requiredPressure;
        this.hazardPressure = hazardPressure;

        this.index = new HashMap<>(keys.length * 2);
        for (int i = 0; i < keys.length; i++) this.index.put(keys[i], i);
    }

    private static float[] filled(int size, float value)
    {
        float[] array = new float[size];
        Arrays.fill(array, value);
        return array;
    }

    public int size()
    {
        return keys.length;
    }

    public String key(int species)
    {
        return (species >= 0 && species < keys.length) ? keys[species] : "?";
    }

    public String[] keys()
    {
        return keys;
    }

    public int indexOf(String key)
    {
        if (key == null) return UNKNOWN;
        Integer i = index.get(key.toLowerCase(Locale.ROOT));
        return i == null ? UNKNOWN : i;
    }

    public boolean isValid(int species)
    {
        return species >= 0 && species < keys.length;
    }

    public float molarMass(int species)
    {
        return molarMass[species];
    }

    public float specificHeat(int species)
    {
        return specificHeat[species];
    }

    public float[] molarMassRaw()
    {
        return molarMass;
    }

    public float[] specificHeatRaw()
    {
        return specificHeat;
    }

    public boolean breathable(int species)
    {
        return breathable[species];
    }

    public float requiredPressure(int species)
    {
        return requiredPressure[species];
    }

    public float hazardPressure(int species)
    {
        return hazardPressure[species];
    }

    public int[] remapFrom(List<String> savedKeys)
    {
        int[] remap = new int[savedKeys.size()];
        for (int i = 0; i < remap.length; i++) remap[i] = indexOf(savedKeys.get(i));
        return remap;
    }

    public boolean sameOrderAs(List<String> other)
    {
        if (other.size() != keys.length) return false;
        for (int i = 0; i < keys.length; i++) if (!keys[i].equals(other.get(i))) return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "SpeciesTable" + Arrays.toString(keys);
    }
}
