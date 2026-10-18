package cute.ame.pioneer.Item.Module;

import cute.ame.pioneer.Item.ToolBox.ToolBoxBlock;
import cute.ame.pioneer.Item.ToolBox.ToolBoxBlockEntity;
import cute.ame.pioneer.Registrie.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

public class PlaceModule extends ToolBoxModule
{
    @Override
    public InteractionResult useOn(ItemStack stack, UseOnContext context)
    {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || level.getBlockState(context.getClickedPos()).getBlock() instanceof ToolBoxBlock)
            return InteractionResult.PASS;

        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) return InteractionResult.FAIL;

        BlockPos pos = place.getClickedPos();
        BlockState state = ModBlocks.TOOLBOX.get().getStateForPlacement(place);
        if (!state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.of(player)))
            return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) return InteractionResult.FAIL;
        if (level.getBlockEntity(pos) instanceof ToolBoxBlockEntity toolbox) toolbox.store(stack.copy());

        player.setItemInHand(context.getHand(), ItemStack.EMPTY);
        SoundType sound = state.getSoundType(level, pos, player);
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        return InteractionResult.CONSUME;
    }
}
