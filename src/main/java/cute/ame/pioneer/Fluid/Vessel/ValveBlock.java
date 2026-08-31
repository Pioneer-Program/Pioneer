package cute.ame.pioneer.Fluid.Vessel;

import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ValveBlock extends FluidVesselBlock
{
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ValveBlock()
    {
        super(Variant.VALVE);
        registerDefaultState(getStateDefinition().any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    public static boolean isOpen(BlockState state)
    {
        return !state.getValue(POWERED);
    }

    @Override
    public void neighborChanged(BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos from, boolean moving)
    {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide) return;

        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;

        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);

        if (level instanceof ServerLevel serverLevel)
        {
            FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
            if (data != null) data.graph().invalidate();
        }
    }
}
