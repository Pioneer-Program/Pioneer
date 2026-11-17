package cute.ame.pioneer.Core.Frame;

import java.util.Arrays;
import java.util.function.IntPredicate;

public final class FrameGrid
{
    public static final double PRECISE_EXTENT = 32768.0; // IS SWEAR, IF SOMEONE, INCREASE THIS VALUE, IM GONNA BAN THEM
    /* LIL CONTEXT:
     * SABLE PHYSICS IS F32, AND F32 HAS 24 BITS OF PRECISION, SO IF YOU GO BEYOND 2^24,
     * YOU START LOSING PRECISION, AND THE PHYSICS WILL BREAK.
     * UNDER 2^15 THE FLOAT STEP IS 0.00195 WHICH IS BELOW RAPIERS TOLERANCE, SO IT'S FINE, :>
     * BUT ABOVE 2^15 THE FLOAT STEP IS 0.0039 WHICH IS ABOVE RAPIERS TOLERANCE, SO IT'S NOT FINE. >:(
     *
     * if needed we can decrease the value tho- but kind of useless imo
     */

    private final int radius;
    private final int side;
    private final int spacing;
    private final int y;
    private final int[] byDistance;

    public FrameGrid(int radius, int spacing, int y)
    {
        if (radius < 0) throw new IllegalArgumentException("radius < 0");
        if (spacing <= 0) throw new IllegalArgumentException("spacing <= 0");

        this.radius = radius;
        this.side = 2 * radius + 1;
        this.spacing = spacing;
        this.y = y;

        Integer[] order = new Integer[side * side];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) ->
        {
            int da = ix(a) * ix(a) + iz(a) * iz(a);
            int db = ix(b) * ix(b) + iz(b) * iz(b);
            if (da != db) return Integer.compare(da, db);
            if (ix(a) != ix(b)) return Integer.compare(ix(a), ix(b));
            return Integer.compare(iz(a), iz(b));
        });

        this.byDistance = new int[order.length];
        for (int i = 0; i < order.length; i++) byDistance[i] = order[i];
    }

    public int radius()
    {
        return radius;
    }

    public int side()
    {
        return side;
    }

    public int spacing()
    {
        return spacing;
    }

    public int y()
    {
        return y;
    }

    public int cellCount()
    {
        return side * side;
    }

    public boolean sameAs(int radius, int spacing, int y)
    {
        return this.radius == radius && this.spacing == spacing && this.y == y;
    }

    public int ix(int cell)
    {
        return cell % side - radius;
    }

    public int iz(int cell)
    {
        return cell / side - radius;
    }

    public int cell(int ix, int iz)
    {
        if (ix < -radius || ix > radius || iz < -radius || iz > radius) return -1;
        return (iz + radius) * side + (ix + radius);
    }

    public double centerX(int cell)
    {
        return (double) ix(cell) * spacing;
    }

    public double centerY()
    {
        return y;
    }

    public double centerZ(int cell)
    {
        return (double) iz(cell) * spacing;
    }

    public int cellAt(double x, double z)
    {
        double fx = Math.floor(x / spacing + 0.5);
        double fz = Math.floor(z / spacing + 0.5);
        if (!(fx >= -radius && fx <= radius && fz >= -radius && fz <= radius)) return -1;

        return cell((int) fx, (int) fz);
    }

    public double halfCell()
    {
        return spacing * 0.5;
    }

    public double extent()
    {
        return radius * (double) spacing + spacing * 0.5;
    }

    public boolean precise()
    {
        return extent() <= PRECISE_EXTENT;
    }

    public int nearestFree(IntPredicate usable)
    {
        for (int cell : byDistance) if (usable.test(cell)) return cell;
        return -1;
    }
}
