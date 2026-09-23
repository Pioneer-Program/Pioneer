package cute.ame.pioneer.Core.Thermal;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Helper.AmbientResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

public final class BlockTemperature
{
    public static final double UNKNOWN = Double.NaN;

    @FunctionalInterface
    public interface Source
    {
        double temperatureAt(ServerLevel level, BlockPos pos);
    }

    private static volatile Source[] SOURCES = new Source[0];

    public static synchronized void register(Source source)
    {
        if (source == null) return;

        Source[] next = Arrays.copyOf(SOURCES, SOURCES.length + 1);
        next[next.length - 1] = source;
        SOURCES = next;
    }

    public static float of(ServerLevel level, BlockPos pos)
    {
        Source[] sources = SOURCES;
        for (int i = 0; i < sources.length; i++)
        {
            double answer = sources[i].temperatureAt(level, pos);
            if (!Double.isNaN(answer)) return (float) answer;
        }

        return dimensionDefault(level);
    }

    public static float dimensionDefault(ServerLevel level)
    {
        AmbientState ambient = AmbientResolver.of(level.dimension());
        if (ambient.vacuum()) return Config.VACUUM_BLOCK_TEMPERATURE_K.get().floatValue();

        return ambient.temperatureK();
    }
}
