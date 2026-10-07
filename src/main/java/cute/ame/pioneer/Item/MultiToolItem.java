package cute.ame.pioneer.Item;

import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Fluid.Block.MassSpectrometerBlock;
import cute.ame.pioneer.Fluid.Block.PipeBlock;
import cute.ame.pioneer.Fluid.Block.ScrubberBlock;
import cute.ame.pioneer.Fluid.Block.VentBlock;
import cute.ame.pioneer.Fluid.Block.VentMode;
import cute.ame.pioneer.Fluid.BlockEntity.MassSpectrometerBlockEntity;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import cute.ame.pioneer.Registrie.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiToolItem extends Item
{
    public MultiToolItem(Properties properties)
    {
        super(properties);
    }

    public static MultiToolMode modeOf(ItemStack stack)
    {
        return stack.getOrDefault(ModDataComponents.MULTITOOL_MODE.get(), MultiToolMode.CONFIGURE);
    }

    public static void setMode(ItemStack stack, MultiToolMode mode)
    {
        stack.set(ModDataComponents.MULTITOOL_MODE.get(), mode);
    }

    public static ItemStack held(Player player)
    {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof MultiToolItem) return main;

        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof MultiToolItem ? off : ItemStack.EMPTY;
    }

    public static void cycleMode(Player player, boolean forward)
    {
        ItemStack stack = held(player);
        if (!stack.isEmpty()) setMode(stack, modeOf(stack).cycle(forward));
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

    private static Component vent(Level level, BlockPos pos, BlockState state, boolean back)
    {
        VentMode next = state.getValue(VentBlock.MODE).cycle(back);
        VentBlock.setMode(level, pos, state, next);

        return Component.translatable("vent.pioneer.mode", Component.translatable("vent.pioneer.mode." + next.getSerializedName()));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        tooltip.add(Component.translatable("multitool.pioneer.mode", modeOf(stack).title()));
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context)
    {
        MultiToolMode mode = modeOf(stack);
        if (mode == MultiToolMode.ROTATE) return MultiToolRotation.use(context);
        if (mode != MultiToolMode.CONFIGURE) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof PipeBlock) && !(block instanceof ScrubberBlock) && !(block instanceof MassSpectrometerBlock) && !(block instanceof VentBlock))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Player player = context.getPlayer();
        boolean back = context.isSecondaryUseActive();

        Component message = switch (block)
        {
            case PipeBlock pipeBlock -> pipe(level, pos, state, context);
            case ScrubberBlock scrubberBlock -> scrubber(level, pos, back);
            case VentBlock ventBlock -> vent(level, pos, state, back);
            default -> spectrometer(level, pos, back);
        };

        if (message != null && player != null) player.displayClientMessage(message, true);
        return InteractionResult.CONSUME;
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
