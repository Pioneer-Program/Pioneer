package cute.ame.pioneer.Thermal.Helper;

import cute.ame.pioneer.Thermal.Data.ThermalStore;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.UnaryOperator;

public final class ThermalRelocation implements AutoCloseable
{
    private final @Nullable ThermalLevelData data;

    private long[] targets;
    private float[] kelvin;
    private int count;

    private ThermalRelocation(@Nullable ThermalLevelData data, int expected)
    {
        this.data = data;
        int capacity = Math.max(expected, 16);
        this.targets = new long[capacity];
        this.kelvin = new float[capacity];
    }

    public static ThermalRelocation open(ServerLevel level, Iterable<BlockPos> blocks, UnaryOperator<BlockPos> route, int expected)
    {
        ThermalLevelData data = ThermalLevelData.getIfPresent(level);
        if (data == null || data.count() == 0) return new ThermalRelocation(null, 0);

        ThermalStore store = data.store();
        ThermalRelocation scope = new ThermalRelocation(data, Math.min(expected, store.count()));

        for (BlockPos pos : blocks)
        {
            long from = pos.asLong();
            int slot = store.slot(from);
            if (slot == ThermalStore.INVALID) continue;

            scope.record(route.apply(pos).asLong(), store.kelvin(slot));
            store.remove(from);
        }

        return scope;
    }

    public int moved()
    {
        return count;
    }

    private void record(long target, float value)
    {
        if (count == targets.length)
        {
            targets = Arrays.copyOf(targets, count << 1);
            kelvin = Arrays.copyOf(kelvin, count << 1);
        }

        targets[count] = target;
        kelvin[count] = value;
        count++;
    }

    @Override
    public void close()
    {
        if (data == null || count == 0) return;

        for (int i = 0; i < count; i++) data.force(targets[i], kelvin[i]);
        count = 0;
    }
}
