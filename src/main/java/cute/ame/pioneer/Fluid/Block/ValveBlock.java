package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ValveBlock extends FluidVesselBlock
{
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final String CHANNEL_OPEN = "open";

    public ValveBlock()
    {
        super(metal(2.0f));
        registerDefaultState(getStateDefinition().any().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(OPEN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(OPEN, true);
    }

    public static boolean isOpen(BlockState state)
    {
        return state.getValue(OPEN);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit)
    {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        setOpen(level, pos, state, !isOpen(state));
        player.displayClientMessage(Component.translatable(isOpen(level.getBlockState(pos)) ? "valve.pioneer.opened" : "valve.pioneer.closed"), true);
        return InteractionResult.CONSUME;
    }

    public static boolean setOpen(Level level, BlockPos pos, BlockState state, boolean open)
    {
        if (!(state.getBlock() instanceof ValveBlock)) return false;
        if (isOpen(state) == open) return false;

        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4f, 1.4f);

        if (level instanceof ServerLevel serverLevel)
        {
            FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
            if (data != null) data.graph().invalidate();
        }

        return true;
    }

    @Override
    public float getVolumeLitres()
    {
        return 50.0f;
    }

    @Override
    public boolean merges()
    {
        return false;
    }

    @Override
    public float getConductance()
    {
        return 0.5f;
    }

    @Override
    public float getNominalBurstPressure()
    {
        return 15.0f;
    }
}
