package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Item.Module.MultiToolModule;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents
{
  public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Pioneer.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MultiToolModule>> MULTITOOL_MODE = DATA_COMPONENTS.register("multitool_mode", () -> DataComponentType.<MultiToolModule>builder().persistent(ModMultiToolModules.CODEC).networkSynchronized(ByteBufCodecs.registry(ModMultiToolModules.KEY)).build());
}
