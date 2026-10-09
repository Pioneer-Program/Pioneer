package cute.ame.pioneer.Item.Module;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Objects;

public class RotateModule extends MultiToolModule
{
    public static final TagKey<Block> ROTATABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_rotatable"));

    private static final EnumProperty<AttachFace> ATTACH = BlockStateProperties.ATTACH_FACE;
    private static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Direction.Axis[] AXES = Direction.Axis.values();

    @Override
    public InteractionResult useOn(ItemStack stack, UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ROTATABLE)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockState next = turn(state, context.getClickedFace(), context.isSecondaryUseActive());
        if (next != state && next.canSurvive(level, pos) && !movesMouth(pos, state, next)) apply(level, pos, next);

        return InteractionResult.CONSUME;
    }

    public static BlockState turn(BlockState state, Direction face, boolean reversed)
    {
        return turn(state, face.getAxis(), (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) != reversed);
    }

    public static BlockState turn(BlockState state, Direction.Axis around, boolean clockwise)
    {
        if (state.hasProperty(ATTACH) && state.hasProperty(HORIZONTAL_FACING))
            return turnAttached(state, around, clockwise);
        if (state.hasProperty(FACING)) return state.setValue(FACING, turn(state.getValue(FACING), around, clockwise));
        if (state.hasProperty(HORIZONTAL_FACING))
        {
            Direction facing = turn(state.getValue(HORIZONTAL_FACING), around, clockwise);
            return facing.getAxis().isVertical() ? state : state.setValue(HORIZONTAL_FACING, facing);
        }

        if (state.hasProperty(AXIS)) return state.setValue(AXIS, turn(state.getValue(AXIS), around));
        if (state.hasProperty(HORIZONTAL_AXIS))
        {
            Direction.Axis axis = turn(state.getValue(HORIZONTAL_AXIS), around);
            return axis.isVertical() ? state : state.setValue(HORIZONTAL_AXIS, axis);
        }

        return state;
    }

    private static BlockState turnAttached(BlockState state, Direction.Axis around, boolean clockwise)
    {
        AttachFace attach = state.getValue(ATTACH);
        Direction facing = state.getValue(HORIZONTAL_FACING);
        Direction normal = turn(attach == AttachFace.FLOOR ? Direction.UP : attach == AttachFace.CEILING ? Direction.DOWN : facing, around, clockwise);
        Direction heading = turn(attach == AttachFace.WALL ? Direction.DOWN : facing, around, clockwise);
        if (normal.getAxis().isHorizontal())
            return state.setValue(ATTACH, AttachFace.WALL).setValue(HORIZONTAL_FACING, normal);

        return state.setValue(ATTACH, normal == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING).setValue(HORIZONTAL_FACING, heading);
    }

    private static Direction turn(Direction direction, Direction.Axis around, boolean clockwise)
    {
        return clockwise ? direction.getClockWise(around) : direction.getCounterClockWise(around);
    }

    private static Direction.Axis turn(Direction.Axis axis, Direction.Axis around)
    {
        return axis == around ? axis : AXES[3 - axis.ordinal() - around.ordinal()];
    }

    public static boolean movesMouth(BlockPos pos, BlockState state, BlockState next)
    {
        if (!(state.getBlock() instanceof FluidVesselBlock vessel)) return false;

        return !Objects.equals(vessel.roomMouth(pos, state), vessel.roomMouth(pos, next));
    }

    private static void apply(Level level, BlockPos pos, BlockState next)
    {
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.3f, 1.8f);
        if (!(next.getBlock() instanceof FluidVesselBlock) || !(level instanceof ServerLevel serverLevel)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
        if (data != null) data.graph().invalidate();
    }
}
