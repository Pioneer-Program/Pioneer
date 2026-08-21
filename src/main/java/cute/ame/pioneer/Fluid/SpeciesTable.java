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

    public SpeciesTable(String[] keys, float[] molarMass, float[] specificHeat)
    {
        this.keys = keys;
        this.molarMass = molarMass;
        this.specificHeat = specificHeat;

        this.index = new HashMap<>(keys.length * 2);
        for (int i = 0; i < keys.length; i++) this.index.put(keys[i], i);
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
