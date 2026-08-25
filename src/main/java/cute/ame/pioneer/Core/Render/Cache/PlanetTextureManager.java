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
    private static final int PLANET_TEXTURE_RESOLUTION = 32;

    private static final TextureManager<CubemapTextures> CACHE = new TextureManager<>(MAX_IDLE_PLANETS);

    public static CubemapTextures getOrGenerate(ProceduralPlanetConfig cfg, String planetId)
    {
        ResourceLocation generatorId = cfg.generatorId();
        String cacheKey = planetId + "@" + cfg.seed() + "_" + generatorId;

        PlanetTextureBaker generator = PlanetTextureRegistry.get(generatorId);

        CubemapTextures group = CACHE.acquire(
            cacheKey,
            CubemapTextures.FACE_NAMES,
            face ->
            {
                Pioneer.LOGGER.debug("[Pioneer] Generating '{}' face {} for '{}' ({}x{}, seed={})", generatorId, CubemapTextures.FACE_NAMES[face], planetId, PLANET_TEXTURE_RESOLUTION, PLANET_TEXTURE_RESOLUTION, cfg.seed());
                return generator.generateFace(cfg, PLANET_TEXTURE_RESOLUTION, face);
            },
            CubemapTextures::of);

        CACHE.release(cacheKey);
        return group;
    }

    public static void invalidateAll() { CACHE.clear(); }

    public static int cachedPlanets() { return CACHE.size(); }
}
