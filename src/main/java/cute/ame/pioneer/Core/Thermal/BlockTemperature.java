package cute.ame.pioneer.Core.Thermal;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Helper.AmbientResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public final class BlockTemperature
{
    public static final double UNKNOWN = Double.NaN;

    @FunctionalInterface
    public interface Source
    {
        double temperatureAt(ServerLevel level, BlockPos pos);
    }

    private static final List<Source> SOURCES = new ArrayList<>(2);

    public static void register(Source source)
    {
        if (source != null) SOURCES.add(source);
    }

    public static float of(ServerLevel level, BlockPos pos)
    {
        for (int i = 0; i < SOURCES.size(); i++)
        {
            double answer = SOURCES.get(i).temperatureAt(level, pos);
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
