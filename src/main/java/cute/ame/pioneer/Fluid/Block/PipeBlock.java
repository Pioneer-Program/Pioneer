package cute.ame.pioneer.Fluid.Block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import cute.ame.pioneer.Fluid.Data.FluidConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PipeBlock extends FluidVesselBlock
{
    public static final MapCodec<PipeBlock> CODEC = MapCodec.unit(PipeBlock::new);

    private static final Direction[] DIRECTIONS = Direction.values();

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION;

    protected final VoxelShape[] shapeByIndex;

    public PipeBlock()
    {
        super(metal(2.0f));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
        this.shapeByIndex = makeShapes();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
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

    //These are for visual purposes only, not node connectivity.
    private BlockState updateConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            boolean connects = canConnectTo(level, neighborPos, direction);
            state = state.setValue(getProperty(direction), connects);
        }
        return state;
    }

    private boolean canConnectTo(LevelAccessor level, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getBlock() instanceof PipeBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connects = canConnectTo(level, neighborPos, direction);
        return state.setValue(getProperty(direction), connects);
    }

    private BooleanProperty getProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST  -> EAST;
            case WEST  -> WEST;
            case UP    -> UP;
            case DOWN  -> DOWN;
        };
    }

    private VoxelShape[] makeShapes() {
        VoxelShape coreShape = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
        VoxelShape[] sideShapes = new VoxelShape[DIRECTIONS.length];

        for (int i = 0; i < DIRECTIONS.length; ++i) {
            Direction direction = DIRECTIONS[i];
            sideShapes[i] = Block.box(
                direction.getStepX() < 0 ? 0.0 : 5.0,
                direction.getStepY() < 0 ? 0.0 : 5.0,
                direction.getStepZ() < 0 ? 0.0 : 5.0,
                direction.getStepX() > 0 ? 16.0 : 11.0,
                direction.getStepY() > 0 ? 16.0 : 11.0,
                direction.getStepZ() > 0 ? 16.0 : 11.0
            );
        }

        VoxelShape[] shapesByMask = new VoxelShape[64];

        for (int k = 0; k < 64; ++k) {
            VoxelShape shape = coreShape;

            for (int j = 0; j < DIRECTIONS.length; ++j) {
                if ((k & (1 << j)) != 0) {
                    shape = Shapes.or(shape, sideShapes[j]);
                }
            }

            shapesByMask[k] = shape;
        }

        return shapesByMask;
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapeByIndex[this.getAABBIndex(state)];
    }

    protected int getAABBIndex(BlockState state) {
        int i = 0;

        for(int j = 0; j < DIRECTIONS.length; ++j) {
            if ((Boolean)state.getValue((Property)PROPERTY_BY_DIRECTION.get(DIRECTIONS[j]))) {
                i |= 1 << j;
            }
        }

        return i;
    }

    static {
        PROPERTY_BY_DIRECTION = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (p_55164_) -> {
            p_55164_.put(Direction.NORTH, NORTH);
            p_55164_.put(Direction.EAST, EAST);
            p_55164_.put(Direction.SOUTH, SOUTH);
            p_55164_.put(Direction.WEST, WEST);
            p_55164_.put(Direction.UP, UP);
            p_55164_.put(Direction.DOWN, DOWN);
        }));
    }
}
