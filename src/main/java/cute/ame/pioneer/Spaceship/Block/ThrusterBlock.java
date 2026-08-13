package cute.ame.pioneer.Spaceship.Block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Engine block. Placed as part of a ship, it pushes the assembled ship in the
 * direction it faces ({@link #FACING}) while it is switched on ({@link #ACTIVE}).
 * <p>
 * V1: activation is a simple right-click toggle (no redstone/fuel yet), and
 * every active thruster contributes the same acceleration, summed by
 * {@code ShipEntity#serverTick()} so stacking aligned thrusters accelerates
 * the ship faster.
 */
public class ThrusterBlock extends DirectionalBlock {

    /**
     * Force produced by one active thruster, in newtons [N], along its {@link #FACING}.
     * <p>
     * Sizing: Sable gives each solid block a default mass of 1.0 (see {@code PhysicsBlockPropertyTypes.MASS})
     * and the default overworld gravity is -11 m/s^2 ({@code DimensionPhysics.DEFAULT_GRAVITY}).
     * So hovering an N-block ship needs roughly {@code N * 11} newtons of total thrust — a 30-block ship
     * needs ~330 N. At 400 N a single thruster already lifts a ~36-block ship, and thrust stacks per
     * active thruster. Lower this if ships feel too twitchy.
     */
    public static final double THRUST = 400.0;

    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    private static final MapCodec<ThrusterBlock> CODEC = simpleCodec(p -> new ThrusterBlock());

    @Override
    public MapCodec<ThrusterBlock> codec() { return CODEC; }

    public ThrusterBlock() {
        super(BlockBehaviour.Properties.of()
            .strength(2.0F, 6.0F)
        );
        registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getClickedFace())
            .setValue(ACTIVE, Boolean.FALSE);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        boolean newActive = !state.getValue(ACTIVE);
        level.setBlock(pos, state.setValue(ACTIVE, newActive), Block.UPDATE_CLIENTS);
        // DEBUG: confirms the toggle actually happened server-side. Safe to delete later.
        player.displayClientMessage(Component.literal("[Pioneer] Thruster at " + pos.toShortString() + " -> " + (newActive ? "ON" : "OFF")), true);
        return InteractionResult.SUCCESS;
    }
}
