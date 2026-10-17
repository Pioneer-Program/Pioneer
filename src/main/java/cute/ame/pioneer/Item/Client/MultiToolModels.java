package cute.ame.pioneer.Item.Client;

import cute.ame.pioneer.Item.Module.MultiToolModule;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Registrie.ModMultiToolModules;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class MultiToolModels
{
    private static final ModelResourceLocation ITEM = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multi_tool"));

    public static ResourceLocation modelOf(MultiToolModule module)
    {
        ResourceLocation key = ModMultiToolModules.REGISTRY.getKey(module);

        assert key != null;
        return key.withPrefix("item/multi_tool/");
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event)
    {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        for (MultiToolModule module : ModMultiToolModules.REGISTRY)
        {
            ResourceLocation model = modelOf(module);
            if (resources.getResource(model.withPath(path -> "models/" + path + ".json")).isPresent())
                event.register(ModelResourceLocation.standalone(model));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event)
    {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel base = models.get(ITEM);
        if (base == null) return;

        Reference2ObjectOpenHashMap<MultiToolModule, BakedModel> byModule = new Reference2ObjectOpenHashMap<>();
        for (MultiToolModule module : ModMultiToolModules.REGISTRY)
        {
            BakedModel model = models.get(ModelResourceLocation.standalone(modelOf(module)));
            if (model != null) byModule.put(module, model);
        }

        models.put(ITEM, new ModuleModel(base, byModule));
    }

    private static final class ModuleModel extends BakedModelWrapper<BakedModel>
    {
        private final ItemOverrides overrides;

        private ModuleModel(BakedModel base, Reference2ObjectOpenHashMap<MultiToolModule, BakedModel> byModule)
        {
            super(base);
            overrides = new ItemOverrides()
            {
                @Override
                public @NotNull BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed)
                {
                    BakedModel found = byModule.get(MultiToolItem.moduleOf(stack));
                    return found != null ? found : model;
                }
            };
        }

        @Override
        public @NotNull ItemOverrides getOverrides()
        {
            return overrides;
        }
    }
}
