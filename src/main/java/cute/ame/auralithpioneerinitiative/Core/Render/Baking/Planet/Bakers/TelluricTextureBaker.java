package cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil.*;

public final class TelluricTextureBaker implements PlanetTextureBaker
{
    @Override
    public NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face)
    {
        NativeImage img = new NativeImage(res, res, false);
        long seed = cfg.seed();
        int oct = cfg.octaves();
        float rough = cfg.roughness();

        float seaLevel = 0.42f + pseudoRandom3(1, 0, 0, seed) * 0.12f;
        float polarStart = 0.55f + pseudoRandom3(2, 0, 0, seed) * 0.15f;
        float polarEnd = 0.85f + pseudoRandom3(3, 0, 0, seed) * 0.10f;

        float[] d = new float[3];
        for (int y = 0; y < res; y++)
        {
            for (int x = 0; x < res; x++)
            {
                NoiseUtil.faceDir(face, x, y, res, d);
                float dx = d[0], dy = d[1], dz = d[2];

                float absDy = Math.abs(dy);
                float iceBlend = smoothStep(clamp01((absDy - polarStart) / (polarEnd - polarStart)));

                float warpScale = 1.2f;
                float wx = fbm3(dx * warpScale, dy * warpScale, dz * warpScale, seed + 11L, 3, 0.5f) * 0.55f;
                float wy = fbm3(dx * warpScale + 3.7f, dy * warpScale + 1.3f, dz * warpScale + 2.8f, seed + 22L, 3, 0.5f) * 0.55f;
                float wz = fbm3(dx * warpScale + 7.1f, dy * warpScale + 5.4f, dz * warpScale + 4.2f, seed + 33L, 3, 0.5f) * 0.55f;

                float continentScale = 1.3f;
                float landBase = fbm3((dx + wx) * continentScale, (dy + wy) * continentScale, (dz + wz) * continentScale, seed, oct, rough);
                float landMask = smoothStep(clamp01((landBase - seaLevel) / 0.08f));
                float detail = fbm3(dx * 5f, dy * 5f, dz * 5f, seed + 1L, 3, 0.45f) * 0.10f;

                float r, g, b;
                if (landMask < 0.05f)
                {
                    float depth = clamp01(1f - landBase / seaLevel);
                    r = lerp(cfg.pr(), cfg.pr() * 0.35f, depth);
                    g = lerp(cfg.pg(), cfg.pg() * 0.40f, depth);
                    b = lerp(cfg.pb(), cfg.pb() * 0.60f, depth);
                }
                else if (landMask < 0.15f)
                {
                    float t = landMask / 0.15f;
                    r = lerp(cfg.pr(), cfg.pr() * 1.15f, t);
                    g = lerp(cfg.pg(), cfg.sg() * 1.10f, t);
                    b = lerp(cfg.pb(), cfg.sb() * 0.45f, t);
                }
                else
                {
                    float elev = clamp01(landBase - seaLevel + detail);
                    if (elev < 0.18f)
                    {
                        r = cfg.sr(); g = cfg.sg(); b = cfg.sb();
                    }
                    else if (elev < 0.40f)
                    {
                        float t = (elev - 0.18f) / 0.22f;
                        r = lerp(cfg.sr(), cfg.sr() * 0.65f, t);
                        g = lerp(cfg.sg(), cfg.sg() * 0.60f, t);
                        b = lerp(cfg.sb(), cfg.sb() * 0.50f, t);
                    }
                    else
                    {
                        float t = clamp01((elev - 0.40f) / 0.30f);
                        r = lerp(0.45f, 0.88f, t);
                        g = lerp(0.42f, 0.88f, t);
                        b = lerp(0.38f, 0.90f, t);
                    }
                }

                float cloud = fbm3(dx * 3.5f + 3.7f, dy * 3.5f + 1.2f, dz * 3.5f + 2.1f, seed + 8192L, 4, 0.50f);
                float cloudAmt = clamp01((cloud - 0.50f) / 0.18f) * 0.45f;
                r = lerp(r, 0.96f, cloudAmt);
                g = lerp(g, 0.96f, cloudAmt);
                b = lerp(b, 0.97f, cloudAmt);

                r = lerp(r, 0.90f, iceBlend);
                g = lerp(g, 0.93f, iceBlend);
                b = lerp(b, 1.00f, iceBlend);

                img.setPixelRGBA(x, y, toRGBA(r, g, b, 1f));
            }
        }
        return img;
    }
}
