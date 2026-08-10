package cute.ame.pioneer;

import cute.ame.pioneer.Core.Render.Baking.LUT.BuiltinLUTs;
import cute.ame.pioneer.Planet.Arid.Particle.AridGroundScatterParticle;
import cute.ame.pioneer.Registrie.ModParticles;
import cute.ame.pioneer.Registrie.ModVfxShaders;
import cute.ame.pioneer.SkyPlanet.Dimension.SpaceDimensionEffect;
import cute.ame.pioneer.SkyPlanet.Loader.SolarSystemLoader;
import cute.ame.pioneer.SkyPlanet.Star.BuiltinStarTypes;
import cute.ame.pioneer.Core.Render.Baking.Planet.BuiltinPlanetTextures;
import cute.ame.pioneer.Core.Render.Cache.PlanetTextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class PioneerClient
{
  static
  {
    BuiltinLUTs.registerAll();
    ModVfxShaders.registersAll();
    BuiltinPlanetTextures.registerAll();
    BuiltinStarTypes.registerAll();
  }

  @SubscribeEvent
  public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event)
  {
    event.registerReloadListener(SolarSystemLoader.INSTANCE);
    event.registerReloadListener((prepBarrier, resourceManager, prepProfiler, applyProfiler, prepExec, applyExec) ->
        prepBarrier.wait(null).thenRunAsync(PlanetTextureManager::invalidateAll, applyExec));
  }

  @SubscribeEvent
  public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event)
  {
    event.register(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "space"), new SpaceDimensionEffect());
  }

  @SubscribeEvent
  public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event)
  {
    event.registerSprite(ModParticles.ARID_GROUND_SCATTER.get(), new AridGroundScatterParticle.Provider());
    Pioneer.LOGGER.debug("[Auralith] Registered particle providers");
  }
}
