package cute.ame.pioneer.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;
import cute.ame.pioneer.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.pioneer.Core.Render.Helper.NoiseUtil.*;

public final class LavaTextureBaker implements PlanetTextureBaker
{
    @Override
    public NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face)
    {
        NativeImage img = new NativeImage(res, res, false);
        long seed = cfg.seed(); int oct = cfg.octaves(); float rough = cfg.roughness();
        float[] d = new float[3];
        for (int y = 0; y < res; y++)
        {
            for (int x = 0; x < res; x++)
            {
                NoiseUtil.faceDir(face, x, y, res, d);
                float dx = d[0], dy = d[1], dz = d[2];
                float base = fbm3(dx * 3f, dy * 3f, dz * 3f, seed, oct, rough);
                float veinMask = clamp01(Math.max(0, 0.35f - base) * 5f);
                float glow = clamp01(veinMask + fbm3(dx * 8f, dy * 8f, dz * 8f, seed + 999L, 3, 0.6f) * 0.3f * veinMask);
                img.setPixelRGBA(x, y, toRGBA(lerp(cfg.sr(), cfg.pr(), glow), lerp(cfg.sg(), cfg.pg(), glow * 0.6f), lerp(cfg.sb(), cfg.pb(), glow * 0.1f), 1f));
            }
        }
        return img;
    }
}
