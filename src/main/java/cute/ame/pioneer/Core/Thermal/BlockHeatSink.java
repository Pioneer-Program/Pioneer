package cute.ame.pioneer.Core.Thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public final class BlockHeatSink
{
    public static final double UNKNOWN = Double.NaN;

    public interface Sink
    {
        double capacityAt(ServerLevel level, BlockPos pos);
        double inject(ServerLevel level, BlockPos pos, double joules);
    }

    private static volatile Sink[] SINKS = new Sink[0];

    public static synchronized void register(Sink sink)
    {
        if (sink == null) return;

        Sink[] next = Arrays.copyOf(SINKS, SINKS.length + 1);
        next[next.length - 1] = sink;
        SINKS = next;
    }

    public static double capacityAt(ServerLevel level, BlockPos pos)
    {
        Sink[] sinks = SINKS;
        for (Sink sink : sinks)
        {
            double capacity = sink.capacityAt(level, pos);
            if (!Double.isNaN(capacity)) return capacity;
        }

        return UNKNOWN;
    }

    public static double inject(ServerLevel level, BlockPos pos, double joules)
    {
        if (joules == 0.0 || !Double.isFinite(joules)) return 0.0;

        Sink[] sinks = SINKS;
        for (Sink sink : sinks)
        {
            double accepted = sink.inject(level, pos, joules);
            if (accepted != 0.0) return accepted;
        }

        return 0.0;
    }
}
