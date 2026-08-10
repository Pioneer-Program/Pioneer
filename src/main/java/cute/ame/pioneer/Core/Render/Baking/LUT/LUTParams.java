package cute.ame.pioneer.Core.Render.Baking.LUT;

import java.util.Arrays;

public record LUTParams(long seed, int resolution, float[] extra)
{
    private static final float[] EMPTY = new float[0];

    public static LUTParams of(long seed, int resolution)
    {
        return new LUTParams(seed, resolution, EMPTY);
    }

    public static LUTParams of(long seed, int resolution, float... extra)
    {
        return new LUTParams(seed, resolution, extra == null ? EMPTY : extra);
    }

    public float extra(int index, float fallback)
    {
        return extra != null && extra.length > index ? extra[index] : fallback;
    }

    public int cacheHash()
    {
        int h = Long.hashCode(seed);
        h = 31 * h + resolution;
        h = 31 * h + Arrays.hashCode(extra);
        return h;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LUTParams other)) return false;
        return seed == other.seed && resolution == other.resolution && Arrays.equals(extra, other.extra);
    }

    @Override
    public int hashCode() { return cacheHash(); }

    @Override
    public String toString()
    {
        return "LUTParams[seed=" + seed + ", res=" + resolution + ", extra=" + Arrays.toString(extra) + "]";
    }
}
