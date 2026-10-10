package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Core.Readout.Readout;
import cute.ame.pioneer.Core.Readout.ReadoutUnit;
import cute.ame.pioneer.Fluid.BlockEntity.MassSpectrometerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MassSpectrometerBlock extends PioneerVesselBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final float SAMPLE_VOLUME_L = 1.0f;

    private static final VoxelShape[] SHAPES = buildShapes();

    public MassSpectrometerBlock()
    {
        super(metal(1.5f).noOcclusion());
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public int ports(BlockState state)
    {
        return port(state.getValue(FACING).getOpposite()) | port(Direction.DOWN);
    }

    @Override
    public @NotNull Direction outlet(BlockState state)
    {
        return Direction.DOWN;
    }

    @Override
    public @NotNull Direction inlet(BlockState state)
    {
        return state.getValue(FACING).getOpposite();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new MassSpectrometerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        if (level.isClientSide) return null;

        return (l, pos, s, be) -> { if (be instanceof MassSpectrometerBlockEntity spectrometer) spectrometer.serverTick(); };
    }

    private static VoxelShape[] buildShapes()
    {
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[Direction.SOUTH.get2DDataValue()] = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 14.0);
        shapes[Direction.WEST.get2DDataValue()] = Block.box(2.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        shapes[Direction.NORTH.get2DDataValue()] = Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 16.0);
        shapes[Direction.EAST.get2DDataValue()] = Block.box(0.0, 0.0, 0.0, 14.0, 16.0, 16.0);
        return shapes;
    }

    @Override
    public float getVolumeLitres()
    {
        return SAMPLE_VOLUME_L;
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
        return 10.0f;
    }

    @Override
    protected void details(ServerLevel level, BlockPos pos, BlockState state, Readout out)
    {
        if (!(level.getBlockEntity(pos) instanceof MassSpectrometerBlockEntity spectrometer)) return;

        out.translated("readout.pioneer.sampling", "gas.pioneer." + spectrometer.getGas());
        out.number("readout.pioneer.reading", spectrometer.reading(), ReadoutUnit.PERCENT);
    }
}
