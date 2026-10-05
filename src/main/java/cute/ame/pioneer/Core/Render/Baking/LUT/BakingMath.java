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

    public static float perlinPeriodic(float x, float y, float z, int period, long seed)
    {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        float fx = x - ix, fy = y - iy, fz = z - iz;
        float gx = fx - 1f, gy = fy - 1f, gz = fz - 1f;
        float ux = NoiseUtil.fade(fx), uy = NoiseUtil.fade(fy), uz = NoiseUtil.fade(fz);

        int x0 = Math.floorMod(ix, period), x1 = Math.floorMod(ix + 1, period);
        int y0 = Math.floorMod(iy, period), y1 = Math.floorMod(iy + 1, period);
        int z0 = Math.floorMod(iz, period), z1 = Math.floorMod(iz + 1, period);

        float n000 = NoiseUtil.grad(NoiseUtil.hash3(x0, y0, z0, seed), fx, fy, fz);
        float n100 = NoiseUtil.grad(NoiseUtil.hash3(x1, y0, z0, seed), gx, fy, fz);
        float n010 = NoiseUtil.grad(NoiseUtil.hash3(x0, y1, z0, seed), fx, gy, fz);
        float n110 = NoiseUtil.grad(NoiseUtil.hash3(x1, y1, z0, seed), gx, gy, fz);
        float n001 = NoiseUtil.grad(NoiseUtil.hash3(x0, y0, z1, seed), fx, fy, gz);
        float n101 = NoiseUtil.grad(NoiseUtil.hash3(x1, y0, z1, seed), gx, fy, gz);
        float n011 = NoiseUtil.grad(NoiseUtil.hash3(x0, y1, z1, seed), fx, gy, gz);
        float n111 = NoiseUtil.grad(NoiseUtil.hash3(x1, y1, z1, seed), gx, gy, gz);

        float a = NoiseUtil.lerp(NoiseUtil.lerp(n000, n100, ux), NoiseUtil.lerp(n010, n110, ux), uy);
        float b = NoiseUtil.lerp(NoiseUtil.lerp(n001, n101, ux), NoiseUtil.lerp(n011, n111, ux), uy);
        return NoiseUtil.lerp(a, b, uz) * 1.1547f;
    }

    public static float perlinFbmPeriodic(float u, float v, float w, int baseFreq, int octaves, long seed)
    {
        float sum = 0f, amp = 0.5f, norm = 0f;
        int freq = baseFreq;

        for (int i = 0; i < octaves; i++)
        {
            sum += perlinPeriodic(u * freq, v * freq, w * freq, freq, seed + i * 13337L) * amp;
            norm += amp;
            amp *= 0.5f;
            freq *= 2;
        }
        return sum / norm;
    }

    public static float worleyFbmPeriodic(float u, float v, float w, int baseCells, long seed)
    {
        float a = 1f - worleyPeriodic(u, v, w, baseCells, seed);
        float b = 1f - worleyPeriodic(u, v, w, baseCells * 2, seed + 0x51ED2701L);
        float c = 1f - worleyPeriodic(u, v, w, baseCells * 4, seed + 0x9E37B31CL);
        return a * 0.625f + b * 0.25f + c * 0.125f;
    }

    public static void equalize(float[] values)
    {
        final int bins = 4096;
        final int[] hist = new int[bins];
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float v : values)
        {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        final float scale = (bins - 1) / Math.max(max - min, 1e-6f);
        for (float v : values) hist[(int) ((v - min) * scale)]++;

        final float[] cdf = new float[bins + 1];
        final float inv = 1f / values.length;
        for (int i = 0, acc = 0; i < bins; i++)
        {
            acc += hist[i];
            cdf[i + 1] = acc * inv;
        }

        for (int i = 0; i < values.length; i++)
        {
            float f = (values[i] - min) * scale;
            int b = (int) f;
            values[i] = NoiseUtil.lerp(cdf[b], cdf[b + 1], f - b);
        }
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
