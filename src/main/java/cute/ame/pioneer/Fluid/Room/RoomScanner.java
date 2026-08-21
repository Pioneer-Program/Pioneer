package cute.ame.pioneer.Fluid.Room;

import java.util.Arrays;

public final class RoomScanner
{
    @FunctionalInterface
    public interface Passable
    {
        boolean test(int x, int y, int z);
    }

    public record Result(boolean sealed, int cellCount, long[] cells, int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        public static final Result NONE = new Result(false, 0, new long[0], 0, 0, 0, 0, 0, 0);

        public boolean isEmpty()
        {
            return cellCount == 0;
        }

        public double volumeLitres(double litresPerBlock)
        {
            return cellCount * litresPerBlock;
        }
    }

    private static final int Y_BITS = 12, Z_BITS = 26, X_BITS = 26;
    private static final long X_MASK = (1L << X_BITS) - 1L;
    private static final long Y_MASK = (1L << Y_BITS) - 1L;
    private static final long Z_MASK = (1L << Z_BITS) - 1L;
    private static final int Y_SHIFT = Z_BITS;
    private static final int X_SHIFT = Z_BITS + Y_BITS;

    public static long pack(int x, int y, int z)
    {
        return ((x & X_MASK) << X_SHIFT) | ((y & Y_MASK) << Y_SHIFT) | (z & Z_MASK);
    }

    public static int unpackX(long p)
    {
        return (int) (p << (64 - X_SHIFT - X_BITS) >> (64 - X_BITS));
    }

    public static int unpackY(long p)
    {
        return (int) (p << (64 - Y_SHIFT - Y_BITS) >> (64 - Y_BITS));
    }

    public static int unpackZ(long p)
    {
        return (int) (p << (64 - Z_BITS) >> (64 - Z_BITS));
    }

    private final int cap;
    private final long[] cells;
    private final int[] table;
    private final int mask;

    public RoomScanner(int capacity)
    {
        this.cap = Math.max(capacity, 1);
        this.cells = new long[cap];

        int size = Integer.highestOneBit(Math.max(cap * 2, 16) - 1) << 1;
        this.table = new int[size];
        this.mask = size - 1;
    }

    public int capacity()
    {
        return cap;
    }

    public Result scan(int originX, int originY, int originZ, Passable passable, int minY, int maxY)
    {
        if (!passable.test(originX, originY, originZ)) return Result.NONE;

        Arrays.fill(table, 0);
        int count = 0;

        int minX = originX, maxX = originX, minZ = originZ, maxZ = originZ;
        int lowY = originY, highY = originY;

        cells[count++] = pack(originX, originY, originZ);
        insert(cells[0], 1);

        boolean sealed = true;

        for (int head = 0; head < count; head++)
        {
            long cell = cells[head];
            int x = unpackX(cell), y = unpackY(cell), z = unpackZ(cell);

            for (int dir = 0; dir < 6; dir++)
            {
                int nx = x + (dir == 0 ? 1 : dir == 1 ? -1 : 0);
                int ny = y + (dir == 2 ? 1 : dir == 3 ? -1 : 0);
                int nz = z + (dir == 4 ? 1 : dir == 5 ? -1 : 0);

                if (ny < minY || ny > maxY)
                {
                    sealed = false;
                    continue;
                }

                if (!passable.test(nx, ny, nz)) continue;

                long packed = pack(nx, ny, nz);
                if (contains(packed)) continue;

                if (count == cap) return new Result(false, count, Arrays.copyOf(cells, count), minX, lowY, minZ, maxX, highY, maxZ);

                cells[count] = packed;
                insert(packed, count + 1);
                count++;

                if (nx < minX) minX = nx;
                else if (nx > maxX) maxX = nx;

                if (ny < lowY) lowY = ny;
                else if (ny > highY) highY = ny;

                if (nz < minZ) minZ = nz;
                else if (nz > maxZ) maxZ = nz;
            }
        }

        return new Result(sealed, count, Arrays.copyOf(cells, count), minX, lowY, minZ, maxX, highY, maxZ);
    }

    private boolean contains(long packed)
    {
        for (int i = index(packed); ; i = (i + 1) & mask)
        {
            int slot = table[i];
            if (slot == 0) return false;
            if (cells[slot - 1] == packed) return true;
        }
    }

    private void insert(long packed, int slot)
    {
        for (int i = index(packed); ; i = (i + 1) & mask)
        {
            if (table[i] != 0) continue;
            table[i] = slot;
            return;
        }
    }

    private int index(long packed)
    {
        long h = packed * 0x9E3779B97F4A7C15L;
        return (int) (h >>> 40) & mask;
    }
}