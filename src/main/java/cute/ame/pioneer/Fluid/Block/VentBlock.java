package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Fluid.Data.FluidConstants;

import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Level.RoomLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class VentBlock extends FluidVesselBlock
{
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape FLOOR_SHAPE = Block.box(3.0, 0.0, 1.0, 13.0, 4.0, 15.0);
    private static final Map<AttachFace, Map<Direction, VoxelShape>> SHAPES = buildShapes();

    public VentBlock()
    {
        super(metal(2.0f));
        registerDefaultState(getStateDefinition().any()
            .setValue(FACE, AttachFace.WALL)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(FACE, FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Direction clicked = context.getClickedFace();

        return switch (clicked)
        {
            case UP -> defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(FACING, context.getHorizontalDirection());
            case DOWN -> defaultBlockState().setValue(FACE, AttachFace.CEILING).setValue(FACING, context.getHorizontalDirection());
            default -> defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, clicked);
        };
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

    public static int attachRoom(ServerLevel level, BlockPos pos, BlockState state)
    {
        return RoomLevelData.get(level).attach(level, mouth(pos, state));
    }

    public static int roomAt(ServerLevel level, BlockPos pos, BlockState state)
    {
        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        return rooms == null ? FluidNodeStore.INVALID : rooms.nodeAt(mouth(pos, state));
    }

    public static void detachRoom(ServerLevel level, BlockPos pos, BlockState state)
    {
        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        if (rooms == null) return;

        BlockPos mouth = mouth(pos, state);
        int nodeId = rooms.nodeAt(mouth);
        if (nodeId == FluidNodeStore.INVALID) return;

        for (Direction direction : Direction.values())
        {
            BlockPos side = mouth.relative(direction);
            if (side.equals(pos)) continue;

            BlockState neighbour = level.getBlockState(side);
            if (!(neighbour.getBlock() instanceof VentBlock)) continue;
            if (roomAt(level, side, neighbour) == nodeId) return;
        }

        rooms.remove(level, nodeId);

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data != null) data.graph().invalidate();
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
        double[] box = { 3.0, 0.0, 1.0, 13.0, 4.0, 15.0 };

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
}
