package cute.ame.pioneer.Core.Configure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface Configurable
{
    @Nullable Component configure(ServerLevel level, BlockPos pos, BlockState state, UseOnContext context);
}
