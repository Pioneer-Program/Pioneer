package cute.ame.pioneer.Core.Render.Baking.Planet.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.SkyPlanet.Data.ProceduralPlanetConfig;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;
import cute.ame.pioneer.Core.Render.Baking.Planet.PlanetTextureBaker;

import static cute.ame.pioneer.Core.Render.Helper.NoiseUtil.*;

public final class TelluricTextureBaker implements PlanetTextureBaker
{
    private static final float CONTINENT_SCALE = 1.15f;
    private static final float WARP_STRENGTH = 0.42f;
    private static final float MOUNTAIN_HEIGHT = 0.30f;

    private static final float SAND_R = 0.80f, SAND_G = 0.71f, SAND_B = 0.50f;
    private static final float DUNE_R = 0.78f, DUNE_G = 0.63f, DUNE_B = 0.40f;
    private static final float DRYG_R = 0.60f, DRYG_G = 0.57f, DRYG_B = 0.33f;
    private static final float TUND_R = 0.52f, TUND_G = 0.52f, TUND_B = 0.46f;
    private static final float ROCK_R = 0.40f, ROCK_G = 0.36f, ROCK_B = 0.32f;
    private static final float SNOW_R = 0.94f, SNOW_G = 0.95f, SNOW_B = 0.99f;
    private static final float ICE_R = 0.86f, ICE_G = 0.91f, ICE_B = 0.99f;

    @Override
    public NativeImage generateFace(ProceduralPlanetConfig cfg, int res, int face)
    {
        final long seed = cfg.seed();
        final int oct = Math.clamp(cfg.octaves(), 3, 9);
        final float rough = Math.clamp(cfg.roughness(), 0.35f, 0.70f);

        final float seaLevel = 0.40f + pseudoRandom3(1, 0, 0, seed) * 0.14f;
        final float capLat = 0.70f + pseudoRandom3(2, 0, 0, seed) * 0.18f;
        final float aridity = 0.34f + pseudoRandom3(3, 0, 0, seed) * 0.32f;
        final float cloudCov = 0.44f + pseudoRandom3(4, 0, 0, seed) * 0.20f;

        final int pitch = res + 2;
        final float[] h = new float[pitch * pitch];
        final float[] d = new float[3];

        for (int py = -1; py <= res; py++)
        {
            int row = (py + 1) * pitch + 1;
            for (int px = -1; px <= res; px++)
            {
                NoiseUtil.faceDir(face, px, py, res, d);
                h[row + px] = terrain(d[0], d[1], d[2], seed, oct, rough, seaLevel);
            }
        }

        final NativeImage img = new NativeImage(res, res, false);
        final float slopeGain = res * 0.45f;

        for (int y = 0; y < res; y++)
        {
            int row = (y + 1) * pitch + 1;
            for (int x = 0; x < res; x++)
            {
                NoiseUtil.faceDir(face, x, y, res, d);
                final float dx = d[0], dy = d[1], dz = d[2];

                final int i = row + x;
                final float height = h[i];
                final float du = h[i + 1] - h[i - 1];
                final float dv = h[i + pitch] - h[i - pitch];
                final float slope = clamp01((float) Math.sqrt(du * du + dv * dv) * slopeGain);

                final float absLat = Math.abs(dy);
                final float elev = height - seaLevel;

                float r, g, b;

                if (elev <= 0f)
                {
                    float depth = clamp01(-elev / seaLevel * 1.7f);
                    depth *= depth * (3f - 2f * depth);

                    r = lerp(cfg.pr() * 1.50f, cfg.pr() * 0.26f, depth);
                    g = lerp(cfg.pg() * 1.35f, cfg.pg() * 0.30f, depth);
                    b = lerp(cfg.pb() * 1.15f, cfg.pb() * 0.42f, depth);

                    float shelf = clamp01(1f - depth * 7f);
                    r = lerp(r, 0.24f, shelf * 0.45f);
                    g = lerp(g, 0.62f, shelf * 0.45f);
                    b = lerp(b, 0.66f, shelf * 0.35f);
                }
                else
                {
                    float temp = 1f - absLat * absLat * 1.30f;
                    temp -= elev * 1.60f;
                    temp += fbmP(dx * 1.9f + 51.2f, dy * 1.9f, dz * 1.9f, seed + 0x5EA1L, 3, 0.5f) * 0.14f;

                    float moist = fbmP01(dx * 2.4f, dy * 2.4f + 12.7f, dz * 2.4f, seed + 0x30E5L, 4, 0.5f);
                    moist = lerp(moist, clamp01(1f - elev * 3.0f), 0.35f);

                    r = cfg.sr(); g = cfg.sg(); b = cfg.sb();

                    float lush = clamp01((moist - 0.48f) * 2.6f) * clamp01(temp * 2.2f);
                    r = lerp(r, r * 0.66f, lush);
                    g = lerp(g, g * 0.78f, lush);
                    b = lerp(b, b * 0.62f, lush);

                    float dry = clamp01((aridity - moist) * 3.2f) * clamp01(temp * 1.8f);
                    r = lerp(r, DRYG_R, clamp01(dry * 1.6f));
                    g = lerp(g, DRYG_G, clamp01(dry * 1.6f));
                    b = lerp(b, DRYG_B, clamp01(dry * 1.6f));
                    r = lerp(r, DUNE_R, dry * dry);
                    g = lerp(g, DUNE_G, dry * dry);
                    b = lerp(b, DUNE_B, dry * dry);

                    float cold = clamp01((0.20f - temp) * 3.4f);
                    r = lerp(r, TUND_R, cold);
                    g = lerp(g, TUND_G, cold);
                    b = lerp(b, TUND_B, cold);

                    float rock = clamp01(slope * 1.35f + (elev - 0.14f) * 3.2f);
                    float rockTint = fbmP01(dx * 9f, dy * 9f, dz * 9f, seed + 0x9A0CL, 2, 0.5f) * 0.18f + 0.91f;
                    r = lerp(r, ROCK_R * rockTint, rock);
                    g = lerp(g, ROCK_G * rockTint, rock);
                    b = lerp(b, ROCK_B * rockTint, rock);

                    float snow = clamp01((0.12f - temp) * 4.5f) * (1f - slope * 0.55f);
                    r = lerp(r, SNOW_R, snow);
                    g = lerp(g, SNOW_G, snow);
                    b = lerp(b, SNOW_B, snow);

                    float beach = clamp01(1f - elev * 90f) * (1f - clamp01(slope * 2f));
                    r = lerp(r, SAND_R, beach);
                    g = lerp(g, SAND_G, beach);
                    b = lerp(b, SAND_B, beach);

                    float ao = 1f - slope * 0.32f + clamp01(elev * 1.2f) * 0.06f;
                    r *= ao; g *= ao; b *= ao;
                }

                float capNoise = fbmP(dx * 3.6f, dy * 3.6f, dz * 3.6f, seed + 0x1CEEL, 3, 0.5f) * 0.10f;
                float cap = smoothStep(clamp01((absLat + capNoise - capLat) / 0.13f));
                if (cap > 0f)
                {
                    r = lerp(r, ICE_R, cap);
                    g = lerp(g, ICE_G, cap);
                    b = lerp(b, ICE_B, cap);
                }
                img.setPixelRGBA(x, y, toRGBA(r, g, b, 1f));
            }
        }

        return img;
    }

    private static float terrain(float dx, float dy, float dz, long seed, int oct, float rough, float seaLevel)
    {
        final float wf = 1.6f;
        float wx = fbmP(dx * wf + 17.3f, dy * wf, dz * wf, seed + 0x51A1L, 3, 0.5f);
        float wy = fbmP(dx * wf, dy * wf + 41.7f, dz * wf, seed + 0x51A2L, 3, 0.5f);
        float wz = fbmP(dx * wf, dy * wf, dz * wf + 63.1f, seed + 0x51A3L, 3, 0.5f);

        float px = dx + wx * WARP_STRENGTH;
        float py = dy + wy * WARP_STRENGTH;
        float pz = dz + wz * WARP_STRENGTH;

        float base = fbmP01(px * CONTINENT_SCALE, py * CONTINENT_SCALE, pz * CONTINENT_SCALE, seed, oct, rough);

        float land = base - seaLevel;
        if (land <= -0.02f) return base;

        float m = ridged3(px * 3.4f, py * 3.4f, pz * 3.4f, seed + 0x77E5L, 5, 0.52f);
        float mask = clamp01(land * 8f);
        return base + m * m * mask * mask * MOUNTAIN_HEIGHT;
    }
}