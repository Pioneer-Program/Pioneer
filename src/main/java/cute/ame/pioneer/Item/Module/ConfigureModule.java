package cute.ame.pioneer.Item.Module;

import cute.ame.pioneer.Core.Configure.Configurable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ConfigureModule extends ToolBoxModule
{
    @Override
    public InteractionResult useOn(ItemStack stack, UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof Configurable configurable)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        Component message = configurable.configure(server, pos, state, context);
        Player player = context.getPlayer();
        if (message != null && player != null) player.displayClientMessage(message, true);

        return InteractionResult.CONSUME;
    }
}
