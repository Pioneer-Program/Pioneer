package cute.ame.pioneer.Registrie;

import com.mojang.serialization.Codec;
import cute.ame.pioneer.Item.Module.ConfigureModule;
import cute.ame.pioneer.Item.Module.PlaceModule;
import cute.ame.pioneer.Item.Module.RotateModule;
import cute.ame.pioneer.Item.Module.ToolBoxModule;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public final class ModToolBoxModules
{
    public static final ResourceKey<Registry<ToolBoxModule>> KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "toolbox_module"));

    public static final Registry<ToolBoxModule> REGISTRY = new RegistryBuilder<>(KEY).sync(true).defaultKey(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "configure")).create();

    public static final DeferredRegister<ToolBoxModule> MODULES = DeferredRegister.create(REGISTRY, Pioneer.MODID);

    public static final DeferredHolder<ToolBoxModule, ConfigureModule> CONFIGURE = MODULES.register("configure", ConfigureModule::new);
    public static final Codec<ToolBoxModule> CODEC = Codec.STRING.xmap(ModToolBoxModules::byName, module -> REGISTRY.getKey(module).toString());
    public static final DeferredHolder<ToolBoxModule, ToolBoxModule> NAME = MODULES.register("name", ToolBoxModule::new);
    public static final DeferredHolder<ToolBoxModule, RotateModule> ROTATE = MODULES.register("rotate", RotateModule::new);
    public static final DeferredHolder<ToolBoxModule, PlaceModule> PLACE = MODULES.register("place", PlaceModule::new);

    public static void register(IEventBus bus)
    {
        bus.addListener((NewRegistryEvent event) -> event.register(REGISTRY));
        MODULES.register(bus);
    }

    public static int count()
    {
        return REGISTRY.size();
    }

    public static int indexOf(ToolBoxModule module)
    {
        return REGISTRY.getId(module);
    }

    public static ToolBoxModule byIndex(int index)
    {
        return REGISTRY.byId(Math.floorMod(index, REGISTRY.size()));
    }

    public static ToolBoxModule cycle(ToolBoxModule module, boolean forward)
    {
        return byIndex(indexOf(module) + (forward ? 1 : -1));
    }

    private static ToolBoxModule byName(String name)
    {
        ToolBoxModule module = REGISTRY.get(name.indexOf(':') < 0 ? ResourceLocation.tryBuild(Pioneer.MODID, name) : ResourceLocation.tryParse(name));
        return module != null ? module : CONFIGURE.get();
    }
}
