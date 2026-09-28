package cute.ame.pioneer.Item;

import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Fluid.Block.MassSpectrometerBlock;
import cute.ame.pioneer.Fluid.Block.PipeBlock;
import cute.ame.pioneer.Fluid.Block.ScrubberBlock;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import cute.ame.pioneer.Fluid.BlockEntity.MassSpectrometerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiToolItem extends Item
{
    public MultiToolItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof PipeBlock) && !(block instanceof ScrubberBlock) && !(block instanceof MassSpectrometerBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        boolean back = context.isSecondaryUseActive();

        Component message = null;
        if (block instanceof PipeBlock) message = pipe(level, pos, state, context);
        else if (block instanceof ScrubberBlock) message = scrubber(level, pos, back);
        else message = spectrometer(level, pos, back);

        if (message != null && player != null) player.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
    }

    private static @Nullable Component pipe(Level level, BlockPos pos, BlockState state, UseOnContext context)
    {
        Direction direction = aimedArm(pos, context);
        String side = direction.getSerializedName();

        return switch (PipeBlock.toggle(level, pos, state, direction))
        {
            case PipeBlock.TOGGLE_LINKED -> Component.translatable("multitool.pioneer.linked", side);
            case PipeBlock.TOGGLE_CUT -> Component.translatable("multitool.pioneer.cut", side);
            default -> Component.translatable("multitool.pioneer.nothing", side);
        };
    }

    private static @Nullable Component scrubber(Level level, BlockPos pos, boolean back)
    {
        if (!(level.getBlockEntity(pos) instanceof PioneerVesselBlockEntity vessel)) return null;

        String next = cycle(vessel.getFilter(), back);
        if (next == null) return null;

        vessel.setFilter(next);
        return Component.translatable("scrubber.pioneer.filter", Component.translatable("gas.pioneer." + next));
    }

    private static @Nullable Component spectrometer(Level level, BlockPos pos, boolean back)
    {
        if (!(level.getBlockEntity(pos) instanceof MassSpectrometerBlockEntity sensor)) return null;

        String next = cycle(sensor.getGas(), back);
        if (next == null) return null;

        sensor.setGas(next);
        return Component.translatable("multitool.pioneer.reading", Component.translatable("gas.pioneer." + next));
    }

    private static Direction aimedArm(BlockPos pos, UseOnContext context)
    {
        Vec3 offset = context.getClickLocation().subtract(Vec3.atCenterOf(pos));
        double reach = Math.max(Math.abs(offset.x), Math.max(Math.abs(offset.y), Math.abs(offset.z)));

        if (reach <= PipeBlock.CORE_HALF + 1.0e-3) return context.getClickedFace();
        return Direction.getNearest(offset.x, offset.y, offset.z);
    }

    private static @Nullable String cycle(String current, boolean back)
    {
        String[] keys = FluidSpecies.active().keys();
        if (keys.length == 0) return null;

        int index = 0;
        for (int i = 0; i < keys.length; i++)
        {
            if (keys[i].equals(current))
            {
                index = i;
                break;
            }
        }

        return keys[(index + (back ? keys.length - 1 : 1)) % keys.length];
    }
}
