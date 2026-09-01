package cute.ame.pioneer.Fluid.Block;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.resources.ResourceLocation;

import cute.ame.pioneer.Fluid.BlockEntity.SensorBlockEntity;
import cute.ame.pioneer.Fluid.Helper.SensorReadings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SensorBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock
{
    private static final VoxelShape FLOOR = Block.box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);
    private static final VoxelShape CEILING = Block.box(3.0, 12.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape WALL_NORTH = Block.box(3.0, 3.0, 12.0, 13.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH = Block.box(3.0, 3.0, 0.0, 13.0, 13.0, 4.0);
    private static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 3.0, 4.0, 13.0, 13.0);
    private static final VoxelShape WALL_WEST = Block.box(12.0, 3.0, 3.0, 16.0, 13.0, 13.0);
    public static final String DEFAULT_GAS = "o2";

    protected SensorBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 4.0f).sound(SoundType.COPPER).noOcclusion().pushReaction(PushReaction.DESTROY));

        registerDefaultState(getStateDefinition().any().setValue(FACE, AttachFace.WALL).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(FACE, FACING);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return switch (state.getValue(FACE))
        {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING))
            {
                case NORTH -> WALL_NORTH;
                case SOUTH -> WALL_SOUTH;
                case EAST -> WALL_EAST;
                default -> WALL_WEST;
            };
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos)
    {
        return canAttach(level, pos, getConnectedDirection(state).getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new SensorBlockEntity(pos, state);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, Player player, @NotNull BlockHitResult hit)
    {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SensorBlockEntity sensor)) return InteractionResult.PASS;

        player.displayClientMessage(sensor.describe(), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        if (level.isClientSide) return null;

        return (l, pos, s, be) -> { if (be instanceof SensorBlockEntity sensor) sensor.serverTick();};
    }

    public static Direction dialUp(BlockState state)
    {
        return switch (state.getValue(FACE))
        {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    public abstract ResourceLocation getReadingType();

    public abstract double readAt(ServerLevel level, BlockPos pos, String argument);

    public abstract double getDialMin();
    public abstract double getDialMax();

    public abstract Component describeValue(double value);
}
