package cute.ame.auralithpioneerinitiative;

import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.BuiltinLUTs;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Particle.AridGroundScatterParticle;
import cute.ame.auralithpioneerinitiative.Registrie.ModParticles;
import cute.ame.auralithpioneerinitiative.Registrie.ModVfxShaders;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Dimension.SpaceDimensionEffect;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Loader.SolarSystemLoader;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Star.BuiltinStarTypes;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.BuiltinPlanetTextures;
import cute.ame.auralithpioneerinitiative.Core.Render.Cache.PlanetTextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = Auralithpioneerinitiative.MODID, value = Dist.CLIENT)
public final class AuralithClient
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
    event.register(ResourceLocation.fromNamespaceAndPath(Auralithpioneerinitiative.MODID, "space"), new SpaceDimensionEffect());
  }

  @SubscribeEvent
  public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event)
  {
    event.registerSprite(ModParticles.ARID_GROUND_SCATTER.get(), new AridGroundScatterParticle.Provider());
    Auralithpioneerinitiative.LOGGER.debug("[Auralith] Registered particle providers");
  }
}
