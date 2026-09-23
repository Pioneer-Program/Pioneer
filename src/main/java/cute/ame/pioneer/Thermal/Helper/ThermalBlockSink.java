package cute.ame.pioneer.Thermal.Helper;

import cute.ame.pioneer.Core.Thermal.BlockHeatSink;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Thermal.Data.MaterialTable;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class ThermalBlockSink implements BlockHeatSink.Sink
{
    public static final ThermalBlockSink INSTANCE = new ThermalBlockSink();

    private static final float FLOOR_K = 0.1f;

    @Override
    public double capacityAt(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return BlockHeatSink.UNKNOWN;

        MaterialTable table = ThermalMaterials.table();
        int material = ThermalMaterials.indexOf(state);
        return table.isValid(material) ? table.volumetricHeat(material) : BlockHeatSink.UNKNOWN;
    }

    @Override
    public double inject(ServerLevel level, BlockPos pos, double joules)
    {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return 0.0;

        MaterialTable table = ThermalMaterials.table();
        int material = ThermalMaterials.indexOf(state);
        if (!table.isValid(material)) return 0.0;

        float capacity = table.volumetricHeat(material);
        if (capacity <= 0.0f) return 0.0;

        ThermalLevelData data = ThermalLevelData.get(level);
        float current = data.kelvinAt(pos);
        if (Float.isNaN(current)) current = BlockTemperature.dimensionDefault(level);

        float next = current + (float) (joules / capacity);
        if (next < FLOOR_K) next = FLOOR_K;

        if (!data.force(pos, next)) return 0.0;

        return (next - current) * (double) capacity;
    }
}
