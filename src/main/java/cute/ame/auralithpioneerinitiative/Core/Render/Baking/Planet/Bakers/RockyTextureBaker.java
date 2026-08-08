package cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil.*;

public final class RockyTextureBaker implements PlanetTextureBaker
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
                float height  = fbm3(dx * 3f, dy * 3f,  dz * 3f,  seed, oct, rough);
                float craterN = fbm3(dx * 12f, dy * 12f, dz * 12f, seed + 777L,  3, 0.40f);
                height = clamp01(height + Math.max(0, 0.08f - craterN) * 4f * 0.3f);
                img.setPixelRGBA(x, y, toRGBA(lerp(cfg.sr(), cfg.pr(), height), lerp(cfg.sg(), cfg.pg(), height), lerp(cfg.sb(), cfg.pb(), height), 1f));
            }
        }
        return img;
    }
}
