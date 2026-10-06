package cute.ame.pioneer.Fluid.Block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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

    @SuppressWarnings("unchecked")
    private static final EnumProperty<PipeConnection>[] ARMS = new EnumProperty[]
    {
        DOWN,
        UP,
        NORTH,
        SOUTH,
        WEST,
        EAST
    };

    public static final int TOGGLE_NONE = -1;
    public static final int TOGGLE_CUT = 0;
    public static final int TOGGLE_LINKED = 1;

    public static final double CORE_HALF = 3.0 / 16.0;

    private static final VoxelShape[] SHAPES = buildShapes();

    private final Reference2IntOpenHashMap<BlockState> masks = new Reference2IntOpenHashMap<>();

    public PipeBlock()
    {
        super(metal(2.0f).noOcclusion());

        BlockState base = getStateDefinition().any();
        for (EnumProperty<PipeConnection> arm : ARMS) base = base.setValue(arm, PipeConnection.NONE);
        registerDefaultState(base);

        for (BlockState state : getStateDefinition().getPossibleStates())
        {
            int mask = 0;
            for (int i = 0; i < ARMS.length; i++)
                if (state.getValue(ARMS[i]).isConnected()) mask |= 1 << i;

            masks.put(state, mask);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(ARMS);
    }

    public static EnumProperty<PipeConnection> arm(Direction direction)
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

            boolean link = (direction == target || !precise) && accepts(neighbour, face) && !sealed(level, cursor, face);
            if (link) state = state.setValue(arm(direction), getConnectionType(neighbour, face));
        }

        return state;
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos)
    {
        EnumProperty<PipeConnection> arm = arm(direction);
        PipeConnection linked = state.getValue(arm);
        Direction face = direction.getOpposite();

        boolean next;
        if (sealed(level, pos, direction)) next = false;
        else if (neighbour.getBlock() instanceof PipeBlock pipe) next = (pipe.ports(neighbour) & port(face)) != 0;
        else next = accepts(neighbour, face);
        PipeConnection connection = next ? getConnectionType(neighbour, face) : PipeConnection.NONE;

        return connection == linked ? state : state.setValue(arm, connection);
    }

    public static int toggle(Level level, BlockPos pos, BlockState state, Direction direction)
    {
        if (!(level.getBlockEntity(pos) instanceof PioneerVesselBlockEntity vessel)) return TOGGLE_NONE;

        EnumProperty<PipeConnection> arm = arm(direction);
        if (state.getValue(arm).isConnected())
        {
            vessel.setSealed(vessel.getSealed() | port(direction));
            level.setBlock(pos, state.setValue(arm, PipeConnection.NONE), Block.UPDATE_ALL);
            return TOGGLE_CUT;
        }

        BlockPos side = pos.relative(direction);
        BlockState neighbour = level.getBlockState(side);
        Direction face = direction.getOpposite();
        if (!accepts(neighbour, face)) return TOGGLE_NONE;

        vessel.setSealed(vessel.getSealed() & ~port(direction));
        if (neighbour.getBlock() instanceof PipeBlock && level.getBlockEntity(side) instanceof PioneerVesselBlockEntity other) other.setSealed(other.getSealed() & ~port(face));

        level.setBlock(pos, state.setValue(arm, getConnectionType(neighbour, face)), Block.UPDATE_ALL);
        return TOGGLE_LINKED;
    }

    private static boolean sealed(BlockGetter level, BlockPos pos, Direction face)
    {
        return level.getBlockEntity(pos) instanceof PioneerVesselBlockEntity vessel && (vessel.getSealed() & port(face)) != 0;
    }

    private static boolean accepts(BlockState neighbour, Direction face)
    {
        return getConnectionType(neighbour, face).isConnected();
    }

    private static PipeConnection getConnectionType(BlockState neighborState, Direction face) {
        Block neighborBlock = neighborState.getBlock();
        Direction direction = face.getOpposite();

        if (neighborBlock instanceof PipeBlock) {
            return PipeConnection.PIPE;
        }

        if (neighborBlock instanceof SensorBlock) {
            AttachFace attachFace = neighborState.getValue(SensorBlock.FACE);
            Direction attachedDirection = switch (attachFace) {
                case FLOOR -> Direction.UP;
                case CEILING -> Direction.DOWN;
                case WALL -> neighborState.getValue(SensorBlock.FACING);
            };
            if (attachedDirection == direction) {
                return PipeConnection.RIM;
            }
            return PipeConnection.NONE;
        }

        if (neighborBlock instanceof FluidVesselBlock vessel && (vessel.ports(neighborState) & port(face)) != 0) {
            return PipeConnection.RIM;
        }

        return PipeConnection.NONE;
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
