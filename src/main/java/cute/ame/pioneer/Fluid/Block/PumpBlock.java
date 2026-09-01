package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Fluid.Data.FluidConstants;

import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PumpBlock extends FluidVesselBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PumpBlock()
    {
        super(metal(2.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<net.minecraft.world.level.block.Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public static void invalidate(ServerLevel level, BlockPos pos)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data != null) data.graph().invalidate();
    }

    @Override
    public float getVolumeLitres()
    {
        return FluidConstants.PIPE_VOLUME_L;
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
        return 20.0f;
    }
}
