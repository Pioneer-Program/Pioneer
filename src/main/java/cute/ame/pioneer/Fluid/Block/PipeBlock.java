package cute.ame.pioneer.Fluid.Block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import cute.ame.celsius.Fluid.Data.FluidConstants;

public class PipeBlock extends PioneerVesselBlock
{
    public static final MapCodec<PipeBlock> CODEC = MapCodec.unit(PipeBlock::new);

    public enum PipeConnection implements StringRepresentable {
        NONE("none"),
        PIPE("pipe"),
        RIM("rim");

        private final String name;

        PipeConnection(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public boolean isConnected() {
            return this != NONE;
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    public static final EnumProperty<PipeConnection> NORTH = EnumProperty.create("north", PipeConnection.class);
    public static final EnumProperty<PipeConnection> EAST  = EnumProperty.create("east", PipeConnection.class);
    public static final EnumProperty<PipeConnection> SOUTH = EnumProperty.create("south", PipeConnection.class);
    public static final EnumProperty<PipeConnection> WEST  = EnumProperty.create("west", PipeConnection.class);
    public static final EnumProperty<PipeConnection> UP    = EnumProperty.create("up", PipeConnection.class);
    public static final EnumProperty<PipeConnection> DOWN  = EnumProperty.create("down", PipeConnection.class);

    public static final Map<Direction, EnumProperty<PipeConnection>> PROPERTY_BY_DIRECTION;

    protected final VoxelShape[] shapeByIndex;

    public PipeBlock()
    {
        super(metal(2.0f));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, PipeConnection.NONE).setValue(EAST, PipeConnection.NONE)
                .setValue(SOUTH, PipeConnection.NONE).setValue(WEST, PipeConnection.NONE)
                .setValue(UP, PipeConnection.NONE).setValue(DOWN, PipeConnection.NONE));
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
            PipeConnection connection = getConnectionType(level, neighborPos, direction);
            state = state.setValue(getProperty(direction), connection);
        }
        return state;
    }

    private PipeConnection getConnectionType(LevelAccessor level, BlockPos neighborPos, Direction direction) {
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (neighborBlock instanceof PipeBlock) {
            return PipeConnection.PIPE;
        }

        if (neighborBlock instanceof SensorBlock) {
            AttachFace face = neighborState.getValue(SensorBlock.FACE);
            Direction attachedDirection = switch (face) {
                case FLOOR -> Direction.UP;
                case CEILING -> Direction.DOWN;
                case WALL -> neighborState.getValue(SensorBlock.FACING);
            };
            if (attachedDirection == direction) {
                return PipeConnection.RIM;
            }
            return PipeConnection.NONE;
        }

        if (neighborBlock instanceof FluidVesselBlock) {
            return PipeConnection.RIM;
        }

        return PipeConnection.NONE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        PipeConnection connection = getConnectionType(level, neighborPos, direction);
        return state.setValue(getProperty(direction), connection);
    }

    private EnumProperty<PipeConnection> getProperty(Direction dir) {
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

        for (int j = 0; j < DIRECTIONS.length; ++j) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(DIRECTIONS[j])).isConnected()) {
                i |= 1 << j;
            }
        }

        return i;
    }

    static {
        PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(Util.make(Maps.newEnumMap(Direction.class), (p_55164_) -> {
            p_55164_.put(Direction.NORTH, NORTH);
            p_55164_.put(Direction.EAST, EAST);
            p_55164_.put(Direction.SOUTH, SOUTH);
            p_55164_.put(Direction.WEST, WEST);
            p_55164_.put(Direction.UP, UP);
            p_55164_.put(Direction.DOWN, DOWN);
        }));
    }
}
