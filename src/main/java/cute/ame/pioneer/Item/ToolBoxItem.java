package cute.ame.pioneer.Item;

import cute.ame.pioneer.Item.Module.ToolBoxModule;
import cute.ame.pioneer.Registrie.ModDataComponents;
import cute.ame.pioneer.Registrie.ModToolBoxModules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ToolBoxItem extends Item
{
    public ToolBoxItem(Properties properties)
    {
        super(properties);
    }

    public static ToolBoxModule moduleOf(ItemStack stack)
    {
        return stack.getOrDefault(ModDataComponents.TOOLBOX_MODE.get(), ModToolBoxModules.PLACE.get());
    }

    public static void setModule(ItemStack stack, ToolBoxModule module)
    {
        stack.set(ModDataComponents.TOOLBOX_MODE.get(), module);
    }

    public static ItemStack held(Player player)
    {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ToolBoxItem) return main;

        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof ToolBoxItem ? off : ItemStack.EMPTY;
    }

    public static void cycleMode(Player player, boolean forward)
    {
        ItemStack stack = held(player);
        if (!stack.isEmpty()) setModule(stack, ModToolBoxModules.cycle(moduleOf(stack), forward));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        tooltip.add(Component.translatable("toolbox.pioneer.mode", moduleOf(stack).title()));
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context)
    {
        return moduleOf(stack).useOn(stack, context);
    }
}
