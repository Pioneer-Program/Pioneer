package cute.ame.pioneer.Fluid.Vessel;

import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Room.RoomLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VentBlock extends FluidVesselBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public VentBlock()
    {
        super(Variant.VENT);
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

    public static BlockPos mouth(BlockPos pos, BlockState state)
    {
        return pos.relative(state.getValue(FACING));
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

        int nodeId = rooms.nodeAt(mouth(pos, state));
        if (nodeId == FluidNodeStore.INVALID) return;

        for (Direction direction : Direction.values())
        {
            BlockPos side = mouth(pos, state).relative(direction);
            if (side.equals(pos)) continue;

            BlockState neighbour = level.getBlockState(side);
            if (!(neighbour.getBlock() instanceof VentBlock)) continue;
            if (roomAt(level, side, neighbour) == nodeId) return;
        }

        rooms.remove(level, nodeId);

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data != null) data.graph().invalidate();
    }
}
