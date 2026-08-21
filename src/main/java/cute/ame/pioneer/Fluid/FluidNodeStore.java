package cute.ame.pioneer.Fluid;

import java.util.Arrays;

public final class FluidNodeStore
{
    public static final int INVALID = -1;

    public static final int FLAG_ALIVE = 1;
    public static final int FLAG_OPEN = 1 << 1;
    public static final int FLAG_LIQUID = 1 << 2;
    public static final int FLAG_DIRTY = 1 << 3;

    private static final int MIN_CAPACITY = 64;

    private int stride;

    private float[] amount; // id * getStride + species in mol
    private float[] moles;
    private float[] volume; // litres
    private float[] temperature; // kelvin
    private int[] flags;
    private int[] generation;

    private int[] free;
    private int freeCount;

    private int capacity;
    private int highWater;
    private int liveCount;

    public FluidNodeStore(int speciesCount)
    {
        this(speciesCount, MIN_CAPACITY);
    }

    public FluidNodeStore(int speciesCount, int initialCapacity)
    {
        stride = Math.max(speciesCount, 0);
        capacity = Math.max(MIN_CAPACITY, initialCapacity);
        amount = new float[capacity * stride];
        moles = new float[capacity];
        volume = new float[capacity];
        temperature = new float[capacity];
        flags = new int[capacity];
        generation = new int[capacity];
        free = new int[32];
    }

    public int getStride()
    {
        return stride;
    }

    public int getGeneration(int id)
    {
        return generation[id];
    }

    public int getLiveCount()
    {
        return liveCount;
    }

    public int getHighWater()
    {
        return highWater;
    }

    public int getCapacity()
    {
        return capacity;
    }

    public float[] getAmountsRaw()
    {
        return amount;
    }

    public float[] getMolesRaw()
    {
        return moles;
    }

    public float[] getVolumesRaw()
    {
        return volume;
    }

    public float[] getTemperaturesRaw()
    {
        return temperature;
    }

    public int[] getFlagsRaw()
    {
        return flags;
    }

    public int create(float volumeLitres, float temperatureKelvin)
    {
        int id;
        if (freeCount > 0) id = free[--freeCount];
        else
        {
            if (highWater == capacity) grow(capacity << 1);
            id = highWater++;
        }

        int base = id * stride;
        Arrays.fill(amount, base, base + stride, 0.0f);
        moles[id] = 0.0f;
        volume[id] = Math.max(volumeLitres, FluidConstants.MIN_VOLUME_L);
        temperature[id] = temperatureKelvin;
        flags[id] = FLAG_ALIVE;
        liveCount++;
        return id;
    }

    public int create(float volumeLitres)
    {
        return create(volumeLitres, FluidConstants.DEFAULT_TEMPERATURE_K);
    }

    public boolean destroy(int id)
    {
        if (!alive(id)) return false;

        flags[id] = 0;
        generation[id]++;
        moles[id] = 0.0f;
        liveCount--;

        if (freeCount == free.length) free = Arrays.copyOf(free, free.length << 1);
        free[freeCount++] = id;
        return true;
    }

    public boolean alive(int id)
    {
        return id >= 0 && id < highWater && (flags[id] & FLAG_ALIVE) != 0;
    }

    public long handle(int id)
    {
        return alive(id) ? (((long) generation[id]) << 32) | (id & 0xFFFFFFFFL) : -1L;
    }

    public int resolve(long handle)
    {
        if (handle < 0) return INVALID;
        int id = (int) (handle & 0xFFFFFFFFL);
        if (!alive(id)) return INVALID;
        return generation[id] == (int) (handle >>> 32) ? id : INVALID;
    }

    public float volume(int id)
    {
        return volume[id];
    }

    public void setVolume(int id, float litres)
    {
        volume[id] = Math.max(litres, FluidConstants.MIN_VOLUME_L);
    }

    public float temperature(int id)
    {
        return temperature[id];
    }

    public void setTemperature(int id, float kelvin)
    {
        temperature[id] = kelvin;
    }

    public int flags(int id)
    {
        return flags[id];
    }

    public boolean hasFlag(int id, int flag)
    {
        return (flags[id] & flag) != 0;
    }

    public void setFlag(int id, int flag, boolean on)
    {
        if (on) flags[id] |= flag;
        else flags[id] &= ~flag;
    }

    public float amount(int id, int species)
    {
        return amount[id * stride + species];
    }

    public float moles(int id)
    {
        return moles[id];
    }

    public void setAmount(int id, int species, float mol)
    {
        amount[id * stride + species] = Math.max(mol, 0.0f);
        recomputeMoles(id);
    }

    public void add(int id, int species, float mol)
    {
        int i = id * stride + species;
        float v = amount[i] + mol;
        amount[i] = Math.max(v, 0.0f);
        recomputeMoles(id);
    }

    public void clear(int id)
    {
        int base = id * stride;
        Arrays.fill(amount, base, base + stride, 0.0f);
        moles[id] = 0.0f;
    }

    public float fraction(int id, int species)
    {
        float n = moles[id];
        return n > 0.0f ? amount[id * stride + species] / n : 0.0f;
    }

    public double mass(int id, float[] molarMassRaw)
    {
        int base = id * stride;
        double sum = 0.0;
        for (int s = 0; s < stride; s++) sum += amount[base + s] * molarMassRaw[s];
        return sum;
    }

    public double pressure(int id)
    {
        float v = volume[id];
        if (v <= 0.0f) return 0.0;
        return moles[id] * FluidConstants.R * temperature[id] / v;
    }

    private void recomputeMoles(int id)
    {
        int base = id * stride;
        double sum = 0.0;
        for (int s = 0; s < stride; s++) sum += amount[base + s];
        moles[id] = (float) sum;
    }

    private void grow(int newCapacity)
    {
        amount = Arrays.copyOf(amount, newCapacity * stride);
        moles = Arrays.copyOf(moles, newCapacity);
        volume = Arrays.copyOf(volume, newCapacity);
        temperature = Arrays.copyOf(temperature, newCapacity);
        flags = Arrays.copyOf(flags, newCapacity);
        generation = Arrays.copyOf(generation, newCapacity);
        capacity = newCapacity;
    }

    public void remapSpecies(int[] remap, int newStride)
    {
        if (newStride == stride && isIdentity(remap)) return;

        float[] next = new float[capacity * newStride];
        int oldStride = stride;

        for (int id = 0; id < highWater; id++)
        {
            if ((flags[id] & FLAG_ALIVE) == 0) continue;

            int oldBase = id * oldStride;
            int newBase = id * newStride;
            int limit = Math.min(oldStride, remap.length);

            for (int s = 0; s < limit; s++)
            {
                int target = remap[s];
                if (target < 0 || target >= newStride) continue;
                next[newBase + target] = amount[oldBase + s];
            }
        }

        amount = next;
        stride = newStride;

        for (int id = 0; id < highWater; id++)
            if ((flags[id] & FLAG_ALIVE) != 0) recomputeMoles(id);
    }

    private boolean isIdentity(int[] remap)
    {
        if (remap.length != stride) return false;
        for (int i = 0; i < remap.length; i++) if (remap[i] != i) return false;
        return true;
    }

    public void beginRestore(int expectedHighWater)
    {
        if (expectedHighWater > capacity) grow(Math.max(expectedHighWater, MIN_CAPACITY));
        Arrays.fill(flags, 0, highWater, 0);
        Arrays.fill(moles, 0, highWater, 0.0f);
        highWater = 0;
        liveCount = 0;
        freeCount = 0;
    }

    public void restore(int id, float volumeLitres, float temperatureKelvin, int nodeFlags, int nodeGeneration)
    {
        if (id >= capacity) grow(Math.max(id + 1, capacity << 1));

        int base = id * stride;
        Arrays.fill(amount, base, base + stride, 0.0f);
        moles[id] = 0.0f;
        volume[id] = Math.max(volumeLitres, FluidConstants.MIN_VOLUME_L);
        temperature[id] = temperatureKelvin;
        flags[id] = nodeFlags | FLAG_ALIVE;
        generation[id] = nodeGeneration;

        if (id >= highWater) highWater = id + 1;
        liveCount++;
    }

    public void endRestore()
    {
        freeCount = 0;
        for (int id = highWater - 1; id >= 0; id--)
        {
            if ((flags[id] & FLAG_ALIVE) != 0) continue;
            if (freeCount == free.length) free = Arrays.copyOf(free, free.length << 1);
            free[freeCount++] = id;
        }
    }
}
