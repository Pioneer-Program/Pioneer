package cute.ame.pioneer.Core.Render.Baking.LUT.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.Core.Render.Baking.LUT.BakingMath;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTBaker;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTParams;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;

import java.util.stream.IntStream;

public final class CloudWeatherBaker implements LUTBaker
{
    private static final int CYCLONES = 12;
    private static final int CYCLONE_STRIDE = 6;

    private static final float FLOW_SCALE = 1.15f;
    private static final float FLOW_AMOUNT = 0.42f;
    private static final float MASS_SCALE = 1.9f;
    private static final float FRONT_SCALE = 2.6f;
    private static final float TYPE_SCALE = 3.4f;
    private static final float TYPE_OFF = 19.2f;
    private static final float DETILE_SCALE = 1.2f;

    private static final float MASS_WEIGHT = 1.15f;
    private static final float FRONT_WEIGHT = 0.34f;
    private static final float CYCLONE_WEIGHT = 0.30f;
    private static final float CLIMATE_WEIGHT = 0.20f;
    private static final float CONTRAST = 2.7f;
    private static final float BASE = 0.14f;

    private static float bell(float v, float center, float width)
    {
        float t = (v - center) / width;
        return (float) Math.exp(-t * t);
    }

    private static float[] cyclones(long seed)
    {
        final float[] out = new float[CYCLONES * CYCLONE_STRIDE];

        for (int i = 0, o = 0; i < CYCLONES; i++, o += CYCLONE_STRIDE)
        {
            float hemi = (i & 1) == 0 ? 1f : -1f;
            float sinLat = hemi * (0.32f + 0.50f * NoiseUtil.pseudoRandom3(i, 1, 0, seed));
            float lon = NoiseUtil.pseudoRandom3(i, 2, 0, seed) * (float) Math.TAU;
            float cosLat = (float) Math.sqrt(1f - sinLat * sinLat);
            float radius = 0.30f + 0.32f * NoiseUtil.pseudoRandom3(i, 3, 0, seed);

            out[o] = cosLat * (float) Math.cos(lon);
            out[o + 1] = sinLat;
            out[o + 2] = cosLat * (float) Math.sin(lon);
            out[o + 3] = (float) Math.cos(radius);
            out[o + 4] = hemi * (1.5f + 1.6f * NoiseUtil.pseudoRandom3(i, 4, 0, seed));
            out[o + 5] = 0.55f + 0.45f * NoiseUtil.pseudoRandom3(i, 5, 0, seed);
        }
        return out;
    }

    private static float twist(float[] d, float[] cyclones)
    {
        float spiral = 0f;

        for (int o = 0; o < cyclones.length; o += CYCLONE_STRIDE)
        {
            final float cx = cyclones[o], cy = cyclones[o + 1], cz = cyclones[o + 2], cosR = cyclones[o + 3];
            final float dot = d[0] * cx + d[1] * cy + d[2] * cz;
            if (dot <= cosR) continue;

            final float t = 1f - (1f - dot) / (1f - cosR);
            final float fall = t * t;
            final float angle = cyclones[o + 4] * fall;
            final float s = (float) Math.sin(angle), c = (float) Math.cos(angle), k = (1f - c) * dot;

            final float x = d[0] * c + (cy * d[2] - cz * d[1]) * s + cx * k;
            final float y = d[1] * c + (cz * d[0] - cx * d[2]) * s + cy * k;
            final float z = d[2] * c + (cx * d[1] - cy * d[0]) * s + cz * k;
            d[0] = x; d[1] = y; d[2] = z;

            spiral += cyclones[o + 5] * fall * (1f - NoiseUtil.clamp01((t - 0.93f) * 20f) * 1.6f);
        }
        return spiral;
    }

    @Override
    public NativeImage bake(LUTParams params)
    {
        final int res = params.resolution();
        final long seed = params.seed();
        final float[] cyclones = cyclones(seed);

        final int[] px = new int[res * res];
        final float inv = 1f / (res - 1);

        IntStream.range(0, res).parallel().forEach(y ->
        {
            final float[] dir = new float[3];
            final float v = y * inv;
            final int row = y * res;

            for (int x = 0; x < res; x++)
            {
                BakingMath.octaDecode(x * inv, v, dir);
                final float rx = dir[0], ry = dir[1], rz = dir[2];
                final float spiral = twist(dir, cyclones);
                final float dx = dir[0], dy = dir[1], dz = dir[2];

                float fx = NoiseUtil.fbmP(dx * FLOW_SCALE + 3.1f, dy * FLOW_SCALE, dz * FLOW_SCALE, seed + 0x4F20L, 3, 0.5f);
                float fy = NoiseUtil.fbmP(dx * FLOW_SCALE, dy * FLOW_SCALE + 7.7f, dz * FLOW_SCALE, seed + 0x51A7L, 3, 0.5f);
                float fz = NoiseUtil.fbmP(dx * FLOW_SCALE, dy * FLOW_SCALE, dz * FLOW_SCALE + 5.3f, seed + 0x6B3DL, 3, 0.5f);
                final float qx = dx + fx * FLOW_AMOUNT, qy = dy + fy * FLOW_AMOUNT, qz = dz + fz * FLOW_AMOUNT;

                float mass = NoiseUtil.fbmP(qx * MASS_SCALE, qy * MASS_SCALE, qz * MASS_SCALE, seed, 5, 0.55f);
                float front = 1f - Math.abs(NoiseUtil.fbmP(qx * FRONT_SCALE + 11.3f, qy * FRONT_SCALE, qz * FRONT_SCALE, seed + 0x7C15L, 4, 0.5f)) * 3.2f;

                float a = Math.abs(ry + fy * 0.12f);
                float climate = bell(a, 0.03f, 0.10f) * 1.1f - bell(a, 0.40f, 0.17f) + bell(a, 0.78f, 0.20f) * 0.8f;

                float macro = BASE + mass * MASS_WEIGHT + Math.max(front, -0.6f) * FRONT_WEIGHT + spiral * CYCLONE_WEIGHT + climate * CLIMATE_WEIGHT;
                float coverage = BakingMath.contrast(macro, CONTRAST);

                float type = BakingMath.contrast(NoiseUtil.fbmP01(dx * TYPE_SCALE + TYPE_OFF, dy * TYPE_SCALE + TYPE_OFF, dz * TYPE_SCALE + TYPE_OFF, seed + 977L, 3, 0.5f), 1.7f);

                float wu = NoiseUtil.fbmP01(rx * DETILE_SCALE + 41.7f, ry * DETILE_SCALE, rz * DETILE_SCALE, seed + 0x1B873593L, 2, 0.4f);
                float wv = NoiseUtil.fbmP01(rx * DETILE_SCALE, ry * DETILE_SCALE + 23.9f, rz * DETILE_SCALE, seed + 0x85EBCA6BL, 2, 0.4f);

                px[row + x] = NoiseUtil.toRGBA(coverage, type, wu, wv);
            }
        });

        return BakingMath.blit(px, res, res);
    }
}
