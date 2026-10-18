package cute.ame.pioneer.Core.Configure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface Tunable
{
    void options(ServerLevel level, BlockPos pos, BlockState state, TuneOptions out);

    boolean tune(ServerLevel level, BlockPos pos, BlockState state, int option, double value);
}
