package cute.ame.pioneer.Core.Frame;

public final class FrameBox
{
    public double minX, minY, minZ, maxX, maxY, maxZ;
    private boolean empty = true;

    private static double axisGap(double aMin, double aMax, double bMin, double bMax)
    {
        if (bMin > aMax) return bMin - aMax;
        if (aMin > bMax) return aMin - bMax;
        return 0.0;
    }

    public boolean isEmpty()
    {
        return empty;
    }

    public FrameBox clear()
    {
        empty = true;
        minX = minY = minZ = maxX = maxY = maxZ = 0.0;
        return this;
    }

    public FrameBox set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        empty = false;
        return this;
    }

    public FrameBox include(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        if (empty) return set(minX, minY, minZ, maxX, maxY, maxZ);
        if (minX < this.minX) this.minX = minX;
        if (minY < this.minY) this.minY = minY;
        if (minZ < this.minZ) this.minZ = minZ;
        if (maxX > this.maxX) this.maxX = maxX;
        if (maxY > this.maxY) this.maxY = maxY;
        if (maxZ > this.maxZ) this.maxZ = maxZ;
        return this;
    }

    public double gap(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        if (empty) return Double.POSITIVE_INFINITY;

        double dx = axisGap(this.minX, this.maxX, minX, maxX);
        double dy = axisGap(this.minY, this.maxY, minY, maxY);
        double dz = axisGap(this.minZ, this.maxZ, minZ, maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
