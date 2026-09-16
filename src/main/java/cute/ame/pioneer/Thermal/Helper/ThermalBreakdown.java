package cute.ame.pioneer.Thermal.Helper;

import cute.ame.pioneer.Fluid.Helper.FluidLevels;
import cute.ame.pioneer.Thermal.Data.MaterialTable;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterials;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ThermalBreakdown
{
    private static final int BREAK_EFFECT = 2001;

    public static boolean exceeded(float kelvin, float breakdownK)
    {
        return kelvin > breakdownK;
    }

    public static int apply(ServerLevel level, ThermalLevelData data, LongArrayList candidates)
    {
        MaterialTable table = ThermalMaterials.table();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int broken = 0;

        for (int i = 0, n = candidates.size(); i < n; i++)
        {
            long packed = candidates.getLong(i);
            cursor.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            if (!FluidLevels.isLoaded(level, cursor)) continue;

            float kelvin = data.kelvinAt(packed);
            if (Float.isNaN(kelvin)) continue;

            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) continue;

            int material = ThermalMaterials.indexOf(state);
            if (!table.isValid(material) || !exceeded(kelvin, table.breakdownK(material))) continue;

            BlockState into = ThermalMaterials.breakdownInto(material);
            if (state.getBlock() == into.getBlock())
            {
                data.detach(cursor);
                continue;
            }

            level.levelEvent(BREAK_EFFECT, cursor, Block.getId(state));
            level.setBlock(cursor, into, Block.UPDATE_ALL);
            broken++;
        }

        return broken;
    }
}
