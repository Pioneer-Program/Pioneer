package cute.ame.pioneer.Item.Module;

import cute.ame.pioneer.Registrie.ModMultiToolModules;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class MultiToolModule
{
    private ResourceLocation icon;
    private Component title;

    public InteractionResult useOn(ItemStack stack, UseOnContext context)
    {
        return InteractionResult.PASS;
    }

    public ResourceLocation icon()
    {
        if (icon == null)
        {
            ResourceLocation key = ModMultiToolModules.REGISTRY.getKey(this);

            assert key != null;
            icon = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/gui/multitool/" + key.getPath() + ".png");
        }

        return icon;
    }

    public Component title()
    {
        if (title == null)
        {
            ResourceLocation key = ModMultiToolModules.REGISTRY.getKey(this);
            
            assert key != null;
            title = Component.translatable("multitool." + key.getNamespace() + ".mode." + key.getPath());
        }

        return title;
    }
}
