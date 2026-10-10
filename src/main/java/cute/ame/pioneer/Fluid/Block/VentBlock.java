package cute.ame.pioneer.Fluid.Block;

import cute.ame.celsius.Fluid.Data.FluidConstants;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import cute.ame.pioneer.Core.Readout.Readout;
import cute.ame.pioneer.Core.Readout.ReadoutUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class VentBlock extends PioneerVesselBlock implements SimpleWaterloggedBlock
{
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<VentMode> MODE = EnumProperty.create("mode", VentMode.class);

    public static final String CHANNEL_MODE = "mode";

    private static final VoxelShape FLOOR_SHAPE = Block.box(1.0, 0.0, 3.0, 15.0, 4.0, 13.0);
    private static final Map<AttachFace, Map<Direction, VoxelShape>> SHAPES = buildShapes();

    public VentBlock()
    {
        super(metal(2.0f));
        registerDefaultState(getStateDefinition().any()
            .setValue(FACE, AttachFace.WALL)
            .setValue(FACING, Direction.NORTH)
            .setValue(WATERLOGGED, false)
            .setValue(MODE, VentMode.BIDIRECTIONAL));
    }

    public static boolean setMode(Level level, BlockPos pos, BlockState state, VentMode mode)
    {
        if (!(state.getBlock() instanceof VentBlock)) return false;
        if (state.getValue(MODE) == mode) return false;

        level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.3f, 1.8f);
        return true;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Direction clicked = context.getClickedFace();
        BlockState base = defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);

        return switch (clicked)
        {
            case UP -> base.setValue(FACE, AttachFace.FLOOR).setValue(FACING, context.getHorizontalDirection());
            case DOWN -> base.setValue(FACE, AttachFace.CEILING).setValue(FACING, context.getHorizontalDirection());
            default -> base.setValue(FACE, AttachFace.WALL).setValue(FACING, clicked);
        };
    }

    @Override
    protected @NotNull FluidState getFluidState(BlockState state)
    {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected @NotNull BlockState updateShape(BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighbourPos)
    {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));

        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(FACE, FACING, WATERLOGGED, MODE);
    }

    @Override
    protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston)
    {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!oldState.is(this) || oldState.getValue(MODE) == state.getValue(MODE)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
        if (data != null) data.graph().invalidate();
    }

    @Override
    public int bridgeFlow(BlockState state)
    {
        return state.getValue(MODE).flow();
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPES.get(state.getValue(FACE)).get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos)
    {
        BlockPos support = pos.relative(opening(state).getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, opening(state));
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

    public static Direction opening(BlockState state)
    {
        return switch (state.getValue(FACE))
        {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    public static BlockPos mouth(BlockPos pos, BlockState state)
    {
        return pos.relative(opening(state));
    }

    @Override
    public int ports(BlockState state)
    {
        return ALL_PORTS & ~port(opening(state));
    }

    @Override
    public @NotNull BlockPos roomMouth(BlockPos pos, BlockState state)
    {
        return mouth(pos, state);
    }

    private static Map<AttachFace, Map<Direction, VoxelShape>> buildShapes()
    {
        Map<AttachFace, Map<Direction, VoxelShape>> byFace = new EnumMap<>(AttachFace.class);

        for (AttachFace face : AttachFace.values())
        {
            Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);

            for (Direction facing : Direction.Plane.HORIZONTAL)
            {
                byFacing.put(facing, transform(face, facing));
            }

            byFace.put(face, byFacing);
        }

        return byFace;
    }

    private static VoxelShape transform(AttachFace face, Direction facing)
    {
        double[] box = { 1.0, 0.0, 3.0, 15.0, 4.0, 13.0 };

        double[] placed = switch (face)
        {
            case FLOOR -> box;
            case CEILING -> new double[] { box[0], 16.0 - box[4], box[2], box[3], 16.0 - box[1], box[5] };
            case WALL -> new double[] { box[0], box[2], 16.0 - box[4], box[3], box[5], 16.0 - box[1] };
        };

        return rotate(placed, facing);
    }

    private static VoxelShape rotate(double[] box, Direction facing)
    {
        return switch (facing)
        {
            case NORTH -> Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
            case SOUTH -> Block.box(16.0 - box[3], box[1], 16.0 - box[5], 16.0 - box[0], box[4], 16.0 - box[2]);
            case WEST -> Block.box(box[2], box[1], 16.0 - box[3], box[5], box[4], 16.0 - box[0]);
            case EAST -> Block.box(16.0 - box[5], box[1], box[0], 16.0 - box[2], box[4], box[3]);
            default -> FLOOR_SHAPE;
        };
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

    @Override
    protected void details(ServerLevel level, BlockPos pos, BlockState state, Readout out)
    {
        out.translated("readout.pioneer.mode", "vent.pioneer.mode." + state.getValue(MODE).getSerializedName());

        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (rooms == null || data == null) return;

        int room = rooms.nodeAt(mouth(pos, state));
        if (room != FluidNodeStore.INVALID && data.store().alive(room))
            out.number("readout.pioneer.room_pressure", data.store().pressure(room), ReadoutUnit.PRESSURE);
    }
}
