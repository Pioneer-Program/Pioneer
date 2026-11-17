package cute.ame.pioneer.Core.Readout;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface ReadoutSource
{
    void readout(ServerLevel level, BlockPos pos, BlockState state, Readout out);
}
