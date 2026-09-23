package cute.ame.pioneer.Thermal.Data;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.Arrays;

public final class ThermalStore
{
    public static final int INVALID = -1;
    public static final int UNRESOLVED = -1;
    private static final int MIN_CAPACITY = 64;

    private final Long2IntOpenHashMap slotOf = new Long2IntOpenHashMap(MIN_CAPACITY);

    private long[] position;
    private float[] kelvin;
    private float[] delta;
    private int[] material;
    private int[] stamp;
    private int[] touched;
    private int touchedCount;
    private int pass = 1;
    private long[] detached;
    private int detachedCount;

    private int count;
    private int capacity;
    private int cursor;

    public ThermalStore()
    {
        this(MIN_CAPACITY);
    }

    public ThermalStore(int initialCapacity)
    {
        capacity = Math.max(MIN_CAPACITY, initialCapacity);
        position = new long[capacity];
        kelvin = new float[capacity];
        delta = new float[capacity];
        material = new int[capacity];
        stamp = new int[capacity];
        touched = new int[32];
        detached = new long[32];

        Arrays.fill(material, UNRESOLVED);
        slotOf.defaultReturnValue(INVALID);
    }

    public int count()
    {
        return count;
    }

    public int capacity()
    {
        return capacity;
    }

    public boolean isEmpty()
    {
        return count == 0;
    }

    public int slot(long pos)
    {
        return slotOf.get(pos);
    }

    public boolean contains(long pos)
    {
        return slotOf.get(pos) != INVALID;
    }

    public float kelvinAt(long pos)
    {
        int slot = slotOf.get(pos);
        return slot == INVALID ? Float.NaN : kelvin[slot];
    }

    public long position(int slot)
    {
        return position[slot];
    }

    public float kelvin(int slot)
    {
        return kelvin[slot];
    }

    public void setKelvin(int slot, float value)
    {
        kelvin[slot] = value;
    }

    public int material(int slot)
    {
        return material[slot];
    }

    public void setMaterial(int slot, int index)
    {
        material[slot] = index;
    }

    public long[] positionsRaw()
    {
        return position;
    }

    public float[] kelvinRaw()
    {
        return kelvin;
    }

    public int[] materialsRaw()
    {
        return material;
    }

    public int attach(long pos, float value, int limit)
    {
        if (!Float.isFinite(value)) return INVALID;

        int slot = slotOf.get(pos);
        if (slot != INVALID)
        {
            kelvin[slot] = value;
            return slot;
        }

        if (count >= limit) return INVALID;
        if (count == capacity) grow(capacity << 1);

        slot = count++;
        position[slot] = pos;
        kelvin[slot] = value;
        delta[slot] = 0.0f;
        material[slot] = UNRESOLVED;
        stamp[slot] = 0;
        slotOf.put(pos, slot);
        return slot;
    }

    public boolean remove(long pos)
    {
        int slot = slotOf.remove(pos);
        if (slot == INVALID) return false;

        int last = --count;
        if (slot != last)
        {
            position[slot] = position[last];
            kelvin[slot] = kelvin[last];
            delta[slot] = delta[last];
            material[slot] = material[last];
            stamp[slot] = stamp[last];
            slotOf.put(position[slot], slot);
        }

        position[last] = 0L;
        kelvin[last] = 0.0f;
        delta[last] = 0.0f;
        material[last] = UNRESOLVED;
        stamp[last] = 0;
        if (cursor >= count) cursor = 0;

        return true;
    }

    public void clear()
    {
        slotOf.clear();
        Arrays.fill(material, 0, count, UNRESOLVED);
        Arrays.fill(delta, 0, count, 0.0f);
        Arrays.fill(stamp, 0, count, 0);

        count = 0;
        cursor = 0;
        touchedCount = 0;
        detachedCount = 0;
    }

    public void invalidateMaterials()
    {
        Arrays.fill(material, 0, count, UNRESOLVED);
    }

    public void addDelta(int slot, float kelvinDelta)
    {
        if (stamp[slot] != pass)
        {
            stamp[slot] = pass;
            if (touchedCount == touched.length) touched = Arrays.copyOf(touched, touchedCount << 1);

            touched[touchedCount++] = slot;
        }

        delta[slot] += kelvinDelta;
    }

    public int applyDeltas()
    {
        int applied = touchedCount;
        for (int i = 0; i < touchedCount; i++)
        {
            int slot = touched[i];
            kelvin[slot] += delta[slot];
            delta[slot] = 0.0f;
        }

        touchedCount = 0;

        if (++pass == Integer.MAX_VALUE)
        {
            Arrays.fill(stamp, 0, capacity, 0);
            pass = 1;
        }

        return applied;
    }

    public void markDetached(long pos)
    {
        if (detachedCount == detached.length) detached = Arrays.copyOf(detached, detachedCount << 1);

        detached[detachedCount++] = pos;
    }

    public void keepAlive(int slot)
    {
        stamp[slot] = pass;
    }

    public int lastTouched(int slot)
    {
        return stamp[slot];
    }

    public int pass()
    {
        return pass;
    }

    public int pendingDetached()
    {
        return detachedCount;
    }

    public int flushDetached()
    {
        if (detachedCount == 0) return 0;
        if (touchedCount != 0) applyDeltas();

        int removed = 0;
        for (int i = 0; i < detachedCount; i++) if (remove(detached[i])) removed++;

        detachedCount = 0;
        return removed;
    }

    public int cursor()
    {
        return count == 0 ? 0 : cursor % count;
    }

    public void advance(int by)
    {
        cursor = count == 0 ? 0 : (cursor + by) % count;
    }

    public void beginRestore(int expected)
    {
        if (expected > capacity) grow(Math.max(expected, MIN_CAPACITY));

        clear();
    }

    public void restore(long pos, float value)
    {
        attach(pos, value, Integer.MAX_VALUE);
    }

    public void endRestore()
    {
        cursor = 0;
    }

    private void grow(int newCapacity)
    {
        int previous = capacity;
        position = Arrays.copyOf(position, newCapacity);
        kelvin = Arrays.copyOf(kelvin, newCapacity);
        delta = Arrays.copyOf(delta, newCapacity);
        material = Arrays.copyOf(material, newCapacity);
        stamp = Arrays.copyOf(stamp, newCapacity);
        Arrays.fill(material, previous, newCapacity, UNRESOLVED);

        capacity = newCapacity;
    }
}
