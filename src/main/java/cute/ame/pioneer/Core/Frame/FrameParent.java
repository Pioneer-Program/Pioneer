package cute.ame.pioneer.Core.Frame;

public interface FrameParent
{
    String name();

    double[] positionAt(long tick, double partial, double[] out);

    double[] velocityAt(long tick, double partial, double[] out);

    double[] orientationAt(long tick, double partial, double[] out);

    double[] spinAt(long tick, double[] out);
}
