package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PumpBlock extends FluidVesselBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_RUNNING = "running";

    private static final Direction[] CYCLE = Direction.values();

    private static final double[][] BOXES =
    {
        { 4.0, 4.0, 2.0, 12.0, 12.0, 12.0 },
        { 5.0, 5.0, 0.0, 11.0, 11.0, 16.0 }
    };

    private static final VoxelShape[] SHAPES = buildShapes();

    public PumpBlock()
    {
        super(metal(2.0f).noOcclusion());

        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, Boolean.TRUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<net.minecraft.world.level.block.Block, BlockState> builder)
    {
        builder.add(FACING, ACTIVE);
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection()).setValue(ACTIVE, Boolean.TRUE);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit)
    {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isSecondaryUseActive())
        {
            Direction next = CYCLE[(state.getValue(FACING).ordinal() + 1) % CYCLE.length];
            setFacing(level, pos, state, next);

            player.displayClientMessage(Component.translatable("pump.pioneer.facing", next.getSerializedName()), true);
            return InteractionResult.CONSUME;
        }

        boolean running = !isActive(state);
        setActive(level, pos, state, running);

        player.displayClientMessage(running ? Component.translatable("pump.pioneer.started", Math.round(powerAt(level, pos) * 100.0f) + "%") : Component.translatable("pump.pioneer.stopped"), true);

        return InteractionResult.CONSUME;
    }

    public static float powerAt(BlockGetter level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel ? vessel.getPower() : 1.0f;
    }

    public static boolean isActive(BlockState state)
    {
        return state.getValue(ACTIVE);
    }

    public static boolean setActive(Level level, BlockPos pos, BlockState state, boolean active)
    {
        if (!(state.getBlock() instanceof PumpBlock)) return false;
        if (isActive(state) == active) return false;

        level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, active ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.3f, active ? 1.6f : 1.1f);

        if (level instanceof ServerLevel serverLevel) invalidate(serverLevel, pos);
        return true;
    }

    public static boolean setFacing(Level level, BlockPos pos, BlockState state, Direction facing)
    {
        if (!(state.getBlock() instanceof PumpBlock)) return false;
        if (state.getValue(FACING) == facing) return false;

        level.setBlock(pos, state.setValue(FACING, facing), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.3f, 1.8f);

        if (level instanceof ServerLevel serverLevel) invalidate(serverLevel, pos);
        return true;
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

    public static void invalidate(ServerLevel level, BlockPos pos)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data != null) data.graph().invalidate();
    }

    private static VoxelShape[] buildShapes()
    {
        VoxelShape[] shapes = new VoxelShape[CYCLE.length];

        for (Direction facing : CYCLE)
        {
            VoxelShape shape = Shapes.empty();
            for (double[] box : BOXES) shape = Shapes.or(shape, turn(box, facing));

            shapes[facing.ordinal()] = shape.optimize();
        }

        return shapes;
    }

    private static VoxelShape turn(double[] box, Direction facing)
    {
        return switch (facing)
        {
            case NORTH -> Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
            case EAST -> Block.box(16.0 - box[5], box[1], box[0], 16.0 - box[2], box[4], box[3]);
            case SOUTH -> Block.box(16.0 - box[3], box[1], 16.0 - box[5], 16.0 - box[0], box[4], 16.0 - box[2]);
            case WEST -> Block.box(box[2], box[1], 16.0 - box[3], box[5], box[4], 16.0 - box[0]);
            case DOWN -> Block.box(box[0], box[2], 16.0 - box[4], box[3], box[5], 16.0 - box[1]);
            case UP -> Block.box(box[0], 16.0 - box[5], box[1], box[3], 16.0 - box[2], box[4]);
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
        return 20.0f;
    }
}
