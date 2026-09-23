package cute.ame.pioneer.Thermal.Block;

import cute.ame.pioneer.Thermal.BlockEntity.ThermalActuatorBlockEntity;
import cute.ame.celsius.Thermal.Data.ThermalDevice;
import cute.ame.celsius.Thermal.Registry.ThermalDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThermalActuatorBlock extends Block implements EntityBlock
{
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public static final String CHANNEL_ACTIVE = "active";
    public static final String CHANNEL_SETPOINT = "setpoint";
    private static final float NUDGE_K = 50.0f;

    public ThermalActuatorBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER).lightLevel(state -> state.getValue(ACTIVE) ? 7 : 0));
        registerDefaultState(getStateDefinition().any().setValue(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder)
    {
        builder.add(ACTIVE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(ACTIVE, Boolean.FALSE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new ThermalActuatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        if (level.isClientSide) return null;

        return (tickLevel, pos, tickState, be) ->
        {
            if (be instanceof ThermalActuatorBlockEntity actuator) actuator.serverTick();
        };
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit)
    {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ThermalActuatorBlockEntity actuator)) return InteractionResult.PASS;

        ThermalDevice device = ThermalDevices.of(state);
        if (device == null)
        {
            player.displayClientMessage(Component.translatable("actuator.pioneer.undefined"), true);
            return InteractionResult.CONSUME;
        }

        if (player.isSecondaryUseActive())
        {
            float next = actuator.getSetpoint() + NUDGE_K;
            if (next > device.maxSetpointK()) next = device.minSetpointK();

            actuator.setSetpoint(next);
            player.displayClientMessage(Component.translatable("actuator.pioneer.setpoint", String.format("%.1f", actuator.getSetpoint())), true);
            return InteractionResult.CONSUME;
        }

        boolean running = !isActive(state);
        setActive(level, pos, state, running);
        player.displayClientMessage(running ? Component.translatable("actuator.pioneer.started", String.format("%.1f", actuator.getSetpoint()), Math.round(device.watts())) : Component.translatable("actuator.pioneer.stopped"), true);

        return InteractionResult.CONSUME;
    }

    public static boolean isActive(BlockState state)
    {
        return state.getBlock() instanceof ThermalActuatorBlock && state.getValue(ACTIVE);
    }

    public static boolean setActive(Level level, BlockPos pos, BlockState state, boolean active)
    {
        if (!(state.getBlock() instanceof ThermalActuatorBlock)) return false;
        if (state.getValue(ACTIVE) == active) return false;

        level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, active ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 0.4f, 1.0f);
        return true;
    }

    public static float setpointAt(BlockGetter level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof ThermalActuatorBlockEntity actuator ? actuator.getSetpoint() : Float.NaN;
    }
}
