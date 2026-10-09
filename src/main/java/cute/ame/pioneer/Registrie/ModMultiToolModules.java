package cute.ame.pioneer.Registrie;

import com.mojang.serialization.Codec;
import cute.ame.pioneer.Item.Module.ConfigureModule;
import cute.ame.pioneer.Item.Module.MultiToolModule;
import cute.ame.pioneer.Item.Module.RotateModule;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public final class ModMultiToolModules
{
    public static final ResourceKey<Registry<MultiToolModule>> KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_module"));

    public static final Registry<MultiToolModule> REGISTRY = new RegistryBuilder<>(KEY).sync(true).defaultKey(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "configure")).create();

    public static final DeferredRegister<MultiToolModule> MODULES = DeferredRegister.create(REGISTRY, Pioneer.MODID);

    public static final DeferredHolder<MultiToolModule, ConfigureModule> CONFIGURE = MODULES.register("configure", ConfigureModule::new);
    public static final Codec<MultiToolModule> CODEC = Codec.STRING.xmap(ModMultiToolModules::byName, module -> REGISTRY.getKey(module).toString());
    public static final DeferredHolder<MultiToolModule, MultiToolModule> NAME = MODULES.register("name", MultiToolModule::new);
    public static final DeferredHolder<MultiToolModule, RotateModule> ROTATE = MODULES.register("rotate", RotateModule::new);

    public static void register(IEventBus bus)
    {
        bus.addListener((NewRegistryEvent event) -> event.register(REGISTRY));
        MODULES.register(bus);
    }

    public static int count()
    {
        return REGISTRY.size();
    }

    public static int indexOf(MultiToolModule module)
    {
        return REGISTRY.getId(module);
    }

    public static MultiToolModule byIndex(int index)
    {
        return REGISTRY.byId(Math.floorMod(index, REGISTRY.size()));
    }

    public static MultiToolModule cycle(MultiToolModule module, boolean forward)
    {
        return byIndex(indexOf(module) + (forward ? 1 : -1));
    }

    private static MultiToolModule byName(String name)
    {
        MultiToolModule module = REGISTRY.get(name.indexOf(':') < 0 ? ResourceLocation.tryBuild(Pioneer.MODID, name) : ResourceLocation.tryParse(name));
        return module != null ? module : CONFIGURE.get();
    }
}
