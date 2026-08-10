package cute.ame.pioneer.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;
import cute.ame.pioneer.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.pioneer.Core.Render.Helper.NoiseUtil.*;

public final class GasGiantTextureBaker implements PlanetTextureBaker
{
    @Override
    public NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face)
    {
        NativeImage img = new NativeImage(res, res, false);
        long seed = cfg.seed();
        float numBands = 7f + pseudoRandom3(0, 0, 0, seed) * 5f;
        float[] d = new float[3];
        for (int y = 0; y < res; y++)
        {
            for (int x = 0; x < res; x++)
            {
                NoiseUtil.faceDir(face, x, y, res, d);
                float dx = d[0], dy = d[1], dz = d[2];
                float lat = (dy + 1f) * 0.5f;
                float turb = fbm3(dx * 3f, dy * 4f, dz * 3f, seed + 1L, 5, 0.55f) * 0.25f;
                float band = (float) Math.sin((lat + turb) * Math.PI * numBands);
                float t = clamp01((band + 1f) * 0.5f + fbm3(dx * 5f + turb, dy * 5f, dz * 5f, seed + 99L, 4, 0.5f) * 0.30f - 0.15f);
                img.setPixelRGBA(x, y, toRGBA(lerp(cfg.sr(), cfg.pr(), t), lerp(cfg.sg(), cfg.pg(), t), lerp(cfg.sb(), cfg.pb(), t), 1f));
            }
        }
        return img;
    }
}
