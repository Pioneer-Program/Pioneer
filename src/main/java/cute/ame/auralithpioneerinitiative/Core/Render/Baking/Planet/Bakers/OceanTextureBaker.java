package cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil.*;

public final class OceanTextureBaker implements PlanetTextureBaker
{
    @Override
    public NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face)
    {
        NativeImage img = new NativeImage(res, res, false);
        long seed = cfg.seed(); int oct = cfg.octaves(); float rough = cfg.roughness();
        final float seaLevel = 0.78f;
        float[] d = new float[3];
        for (int y = 0; y < res; y++)
        {
            for (int x = 0; x < res; x++)
            {
                NoiseUtil.faceDir(face, x, y, res, d);
                float dx = d[0], dy = d[1], dz = d[2];
                float height = fbm3(dx * 2.5f, dy * 2.5f, dz * 2.5f, seed, oct, rough);
                float depth  = fbm3(dx * 6f, dy * 6f, dz * 6f, seed + 500L, 3, 0.45f) * 0.08f;
                float r, g, b;
                if (height < seaLevel)
                {
                    float depthT = 1f - (height / seaLevel);
                    r = lerp(cfg.pr(), cfg.pr() * 0.3f, depthT);
                    g = lerp(cfg.pg(), cfg.pg() * 0.4f, depthT);
                    b = lerp(cfg.pb(), cfg.pb() * 0.6f, depthT);
                }
                else
                {
                    float t = (height - seaLevel) / (1f - seaLevel);
                    r = lerp(cfg.pr() * 0.8f, cfg.sr(), t + depth);
                    g = lerp(cfg.pg() * 0.9f, cfg.sg(), t + depth);
                    b = lerp(cfg.pb() * 0.5f, cfg.sb(), t + depth);
                }
                img.setPixelRGBA(x, y, toRGBA(r, g, b, 1f));
            }
        }
        return img;
    }
}
