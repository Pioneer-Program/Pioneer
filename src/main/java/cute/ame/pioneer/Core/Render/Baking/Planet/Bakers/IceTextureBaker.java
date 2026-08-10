package cute.ame.pioneer.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;
import cute.ame.pioneer.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.pioneer.Core.Render.Helper.NoiseUtil.*;

public final class IceTextureBaker implements PlanetTextureBaker
{
    @Override
    public NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face)
    {
        NativeImage img = new NativeImage(res, res, false);
        long seed = cfg.seed();
        float[] d = new float[3];
        for (int y = 0; y < res; y++)
        {
            for (int x = 0; x < res; x++)
            {
                NoiseUtil.faceDir(face, x, y, res, d);
                float dx = d[0], dy = d[1], dz = d[2];
                float base  = fbm3(dx * 4f, dy * 4f, dz * 4f, seed, 6, 0.50f);
                float warpX = fbm3(dx * 8f, dy * 8f, dz * 8f, seed + 11L, 3, 0.5f) * 0.15f;
                float warpY = fbm3(dx * 8f + 5.2f, dy * 8f + 1.3f, dz * 8f + 2.7f, seed + 22L, 3, 0.5f) * 0.15f;
                float crack = fbm3((dx + warpX) * 16f, (dy + warpY) * 16f, (dz + warpX) * 16f, seed + 33L, 2, 0.4f);
                float brightness = clamp01(base * 0.4f + 0.6f - Math.max(0, 0.12f - crack) / 0.12f * 0.5f);
                img.setPixelRGBA(x, y, toRGBA(lerp(cfg.sr(), cfg.pr(), brightness), lerp(cfg.sg(), cfg.pg(), brightness), lerp(cfg.sb(), cfg.pb(), brightness), 1f));
            }
        }
        return img;
    }
}
