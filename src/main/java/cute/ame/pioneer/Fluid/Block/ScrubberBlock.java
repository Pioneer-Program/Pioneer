package cute.ame.pioneer.Fluid.Block;

import cute.ame.celsius.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScrubberBlock extends PioneerVesselBlock
{
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final String DEFAULT_FILTER = "co2";

    private static final int[] FILTERS = buildFilters();

    public ScrubberBlock()
    {
        super(metal(2.0f));
        registerDefaultState(getStateDefinition().any().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(AXIS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation)
    {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    @Override
    public int filterPorts(BlockState state)
    {
        return FILTERS[state.getValue(AXIS).ordinal()];
    }

    @Override
    public @Nullable String filterSpecies(Level level, BlockPos pos, BlockState state)
    {
        return level.getBlockEntity(pos) instanceof PioneerVesselBlockEntity vessel ? vessel.getFilter() : DEFAULT_FILTER;
    }

    @Override
    public float filterRate(Level level, BlockPos pos, BlockState state)
    {
        return Config.SCRUBBER_RATE.get().floatValue();
    }

    private static int[] buildFilters()
    {
        Direction.Axis[] axes = Direction.Axis.values();
        int[] filters = new int[axes.length];

        for (Direction.Axis axis : axes)
        {
            int bulk = port(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)) | port(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
            filters[axis.ordinal()] = ALL_PORTS & ~bulk;
        }
        return filters;
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
        return 10.0f;
    }
}
