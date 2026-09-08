package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Pioneer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Pioneer.MODID);

    public static final Supplier<CreativeModeTab> PIONEER_TAB = CREATIVE_MODE_TAB.register("pioneer_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CERAMIC_TILES.get()))
                    .title(Component.translatable("creativetab.pioneer.pioneer"))
                    .displayItems((itemDisplayParameters, output) -> {
                            output.accept(ModBlocks.CERAMIC_TILES);
                            output.accept(ModItems.ALUMINUM_INGOT);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
