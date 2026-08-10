package cute.ame.pioneer.Core.Render.Cache;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.pioneer.Core.Render.Baking.Planet.PlanetTextureBaker;
import cute.ame.pioneer.Core.Render.Baking.Planet.PlanetTextureRegistry;
import net.minecraft.resources.ResourceLocation;

public final class PlanetTextureManager
{
    private static final int MAX_IDLE_PLANETS = 32;
    private static final int MIN_RES = 32;
    private static final int MAX_RES = 64;

    private static final TextureManager<CubemapTextures> CACHE = new TextureManager<>(MAX_IDLE_PLANETS);

    public static CubemapTextures getOrGenerate(ProceduralPlanetConfig cfg, String planetId)
    {
        ResourceLocation generatorId = cfg.generatorId();
        String cacheKey = planetId + "@" + cfg.seed() + "_" + generatorId;

        PlanetTextureBaker generator = PlanetTextureRegistry.get(generatorId);
        int res = Math.clamp(cfg.resolution(), MIN_RES, MAX_RES);

        CubemapTextures group = CACHE.acquire(
            cacheKey,
            CubemapTextures.FACE_NAMES,
            face ->
            {
                Pioneer.LOGGER.debug("[Auralith] Generating '{}' face {} for '{}' ({}x{}, seed={})",
                    generatorId, CubemapTextures.FACE_NAMES[face], planetId, res, res, cfg.seed());
                return generator.generateFace(cfg, res, face);
            },
            CubemapTextures::of);

        CACHE.release(cacheKey);
        return group;
    }

    public static void invalidateAll() { CACHE.clear(); }

    public static int cachedPlanets() { return CACHE.size(); }
}
