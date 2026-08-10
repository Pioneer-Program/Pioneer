package cute.ame.pioneer.Core.Render.Baking.LUT;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;

public final class BakingMath
{
    public static NativeImage blit(int[] px, int w, int h)
    {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0, i = 0; y < h; y++)
            for (int x = 0; x < w; x++, i++)
                img.setPixelRGBA(x, y, px[i]);

        return img;
    }

    public static void octaDecode(float u, float v, float[] out)
    {
        float x = u * 2f - 1f;
        float z = v * 2f - 1f;
        float y = 1f - Math.abs(x) - Math.abs(z);

        if (y < 0f)
        {
            float ox = x, oz = z;
            x = (1f - Math.abs(oz)) * Math.copySign(1f, ox);
            z = (1f - Math.abs(ox)) * Math.copySign(1f, oz);
        }

        float invLen = 1f / (float) Math.sqrt(x * x + y * y + z * z);
        out[0] = x * invLen;
        out[1] = y * invLen;
        out[2] = z * invLen;
    }

    public static float valueNoisePeriodic(float x, float y, float z, int period, long seed)
    {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        float fx = x - ix, fy = y - iy, fz = z - iz;
        float ux = NoiseUtil.smoothStep(fx), uy = NoiseUtil.smoothStep(fy), uz = NoiseUtil.smoothStep(fz);

        int x0 = Math.floorMod(ix, period), x1 = Math.floorMod(ix + 1, period);
        int y0 = Math.floorMod(iy, period), y1 = Math.floorMod(iy + 1, period);
        int z0 = Math.floorMod(iz, period), z1 = Math.floorMod(iz + 1, period);

        float c000 = NoiseUtil.pseudoRandom3(x0, y0, z0, seed);
        float c100 = NoiseUtil.pseudoRandom3(x1, y0, z0, seed);
        float c010 = NoiseUtil.pseudoRandom3(x0, y1, z0, seed);
        float c110 = NoiseUtil.pseudoRandom3(x1, y1, z0, seed);
        float c001 = NoiseUtil.pseudoRandom3(x0, y0, z1, seed);
        float c101 = NoiseUtil.pseudoRandom3(x1, y0, z1, seed);
        float c011 = NoiseUtil.pseudoRandom3(x0, y1, z1, seed);
        float c111 = NoiseUtil.pseudoRandom3(x1, y1, z1, seed);

        float a = NoiseUtil.lerp(NoiseUtil.lerp(c000, c100, ux), NoiseUtil.lerp(c010, c110, ux), uy);
        float b = NoiseUtil.lerp(NoiseUtil.lerp(c001, c101, ux), NoiseUtil.lerp(c011, c111, ux), uy);
        return NoiseUtil.lerp(a, b, uz);
    }

    public static float fbmPeriodic(float u, float v, float w, int baseFreq, int octaves, long seed)
    {
        float sum = 0f, amp = 0.5f;
        int freq = baseFreq;

        for (int i = 0; i < octaves; i++)
        {
            float n = valueNoisePeriodic(u * freq, v * freq, w * freq, freq, seed + i * 13337L) * 2f - 1f;
            sum += n * amp;
            amp *= 0.5f;
            freq *= 2;
        }
        return NoiseUtil.clamp01(sum * 0.5f + 0.5f);
    }

    public static float worleyPeriodic(float u, float v, float w, int cells, long seed)
    {
        float x = u * cells, y = v * cells, z = w * cells;
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        float minD = Float.MAX_VALUE;

        for (int dz = -1; dz <= 1; dz++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++)
                {
                    int cx = ix + dx, cy = iy + dy, cz = iz + dz;
                    int wx = Math.floorMod(cx, cells), wy = Math.floorMod(cy, cells), wz = Math.floorMod(cz, cells);

                    float px = cx + NoiseUtil.pseudoRandom3(wx, wy, wz, seed);
                    float py = cy + NoiseUtil.pseudoRandom3(wx, wy, wz, seed + 1L);
                    float pz = cz + NoiseUtil.pseudoRandom3(wx, wy, wz, seed + 2L);

                    float ax = px - x, ay = py - y, az = pz - z;
                    float d = ax * ax + ay * ay + az * az;
                    if (d < minD) minD = d;
                }

        return Math.min(1f, (float) Math.sqrt(minD));
    }

    public static float contrast(float v, float amount)
    {
        return NoiseUtil.clamp01((v - 0.5f) * amount + 0.5f);
    }
}
