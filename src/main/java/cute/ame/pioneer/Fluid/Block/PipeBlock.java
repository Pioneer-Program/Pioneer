package cute.ame.pioneer.Fluid.Block;

import com.mojang.serialization.MapCodec;
import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.Data.FluidConstants;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends PioneerVesselBlock
{
    public static final MapCodec<PipeBlock> CODEC = MapCodec.unit(PipeBlock::new);

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final BooleanProperty[] ARMS =
    {
        BlockStateProperties.DOWN,
        BlockStateProperties.UP,
        BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH,
        BlockStateProperties.WEST,
        BlockStateProperties.EAST
    };

    private static final VoxelShape[] SHAPES = buildShapes();

    private final Reference2IntOpenHashMap<BlockState> masks = new Reference2IntOpenHashMap<>();

    public PipeBlock()
    {
        super(metal(2.0f).noOcclusion());

        BlockState base = getStateDefinition().any();
        for (BooleanProperty arm : ARMS) base = base.setValue(arm, Boolean.FALSE);
        registerDefaultState(base);

        for (BlockState state : getStateDefinition().getPossibleStates())
        {
            int mask = 0;
            for (int i = 0; i < ARMS.length; i++)
                if (state.getValue(ARMS[i])) mask |= 1 << i;

            masks.put(state, mask);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(ARMS);
    }

    public static BooleanProperty arm(Direction direction)
    {
        return ARMS[direction.get3DDataValue()];
    }

    @Override
    public int ports(BlockState state)
    {
        return masks.getInt(state);
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPES[masks.getInt(state)];
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction target = context.replacingClickedOnBlock() ? null : context.getClickedFace().getOpposite();
        boolean precise = context.isSecondaryUseActive();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState state = defaultBlockState();

        for (Direction direction : DIRECTIONS)
        {
            cursor.setWithOffset(pos, direction);
            BlockState neighbour = level.getBlockState(cursor);
            Direction face = direction.getOpposite();

            boolean link = (direction == target || !precise) && accepts(neighbour, face);
            if (link) state = state.setValue(arm(direction), Boolean.TRUE);
        }

        return state;
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos)
    {
        BooleanProperty arm = arm(direction);
        boolean linked = state.getValue(arm);
        Direction face = direction.getOpposite();

        boolean next = neighbour.getBlock() instanceof PipeBlock pipe ? (pipe.ports(neighbour) & port(face)) != 0 : accepts(neighbour, face);

        return next == linked ? state : state.setValue(arm, next);
    }

    private static boolean accepts(BlockState neighbour, Direction face)
    {
        if (neighbour.getBlock() instanceof PipeBlock) return true;

        return neighbour.getBlock() instanceof FluidVesselBlock vessel && (vessel.ports(neighbour) & port(face)) != 0;
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation)
    {
        if (rotation == Rotation.NONE) return state;

        BlockState out = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) out = out.setValue(arm(rotation.rotate(direction)), state.getValue(arm(direction)));

        return out;
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror)
    {
        if (mirror == Mirror.NONE) return state;

        BlockState out = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) out = out.setValue(arm(mirror.mirror(direction)), state.getValue(arm(direction)));

        return out;
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

    private static VoxelShape[] buildShapes()
    {
        VoxelShape core = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
        VoxelShape[] arms = new VoxelShape[DIRECTIONS.length];

        for (Direction direction : DIRECTIONS) arms[direction.get3DDataValue()] = Block.box(direction.getStepX() < 0 ? 0.0 : 5.0, direction.getStepY() < 0 ? 0.0 : 5.0, direction.getStepZ() < 0 ? 0.0 : 5.0, direction.getStepX() > 0 ? 16.0 : 11.0, direction.getStepY() > 0 ? 16.0 : 11.0, direction.getStepZ() > 0 ? 16.0 : 11.0);


        VoxelShape[] shapes = new VoxelShape[1 << arms.length];
        for (int mask = 0; mask < shapes.length; mask++)
        {
            VoxelShape shape = core;
            for (int i = 0; i < arms.length; i++)
                if ((mask & (1 << i)) != 0) shape = Shapes.or(shape, arms[i]);

            shapes[mask] = shape.optimize();
        }
        return shapes;
    }
}
