package cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.BakingMath;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTBaker;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTParams;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil;

import java.util.stream.IntStream;

public final class GalaxySkyboxBaker implements LUTBaker
{
    private static final float NEBULA_SCALE = 1.6f;
    private static final float WIDE_SCALE = 0.7f;
    private static final float HAZE_SCALE = 0.45f;
    private static final float CLUSTER_SCALE = 5.0f;

    @Override
    public NativeImage bake(LUTParams params)
    {
        final int res = params.resolution();
        final long seed = params.seed();

        final int[] px = new int[res * res];
        final float inv = 1f / (res - 1);

        IntStream.range(0, res).parallel().forEach(y ->
        {
            float[] dir = new float[3];
            float[] rgb = new float[3];
            float v = y * inv;
            int row = y * res;

            for (int x = 0; x < res; x++)
            {
                BakingMath.octaDecode(x * inv, v, dir);
                float dx = dir[0], dy = dir[1], dz = dir[2];

                rgb[0] = 0.008f; rgb[1] = 0.030f; rgb[2] = 0.046f;

                float nebula = fbm(dx * NEBULA_SCALE, dy * NEBULA_SCALE, dz * NEBULA_SCALE, seed) * 0.65f + fbm(dx * NEBULA_SCALE * 2.3f + 5f, dy * NEBULA_SCALE * 2.3f + 5f, dz * NEBULA_SCALE * 2.3f + 5f, seed) * 0.35f;
                nebula = NoiseUtil.clamp01(nebula);
                nebula *= nebula;

                float wide = fbm(dx * WIDE_SCALE + 3f, dy * WIDE_SCALE + 3f, dz * WIDE_SCALE + 3f, seed);
                float cloud = nebula * NoiseUtil.lerp(0.4f, 1.0f, wide);

                rgb[0] += NoiseUtil.lerp(0.012f, 0.05f, cloud) * cloud * 1.15f;
                rgb[1] += NoiseUtil.lerp(0.044f, 0.21f, cloud) * cloud * 1.15f;
                rgb[2] += NoiseUtil.lerp(0.068f, 0.29f, cloud) * cloud * 1.15f;

                float haze = fbm(dx * HAZE_SCALE + 9f, dy * HAZE_SCALE + 9f, dz * HAZE_SCALE + 9f, seed);
                rgb[0] += 0.006f * haze; rgb[1] += 0.026f * haze; rgb[2] += 0.040f * haze;

                float cax = -0.55f, cay = -0.45f, caz = 0.7f;
                float caLen = 1f / (float) Math.sqrt(cax * cax + cay * cay + caz * caz);
                float cosA = dx * cax * caLen + dy * cay * caLen + dz * caz * caLen;
                float m = (1f - cosA) * 6f;
                float clusterMask = (float) Math.exp(-m * m);
                float clusterNoise = fbm(dx * CLUSTER_SCALE + 21f, dy * CLUSTER_SCALE + 21f, dz * CLUSTER_SCALE + 21f, seed);
                rgb[0] += 0.10f * clusterMask * clusterNoise * 0.6f;
                rgb[1] += 0.17f * clusterMask * clusterNoise * 0.6f;
                rgb[2] += 0.21f * clusterMask * clusterNoise * 0.6f;

                float stars = 0f;
                stars += starLayer(dx, dy, dz, 22f,0.42f, 0.95f, rgb, seed);
                stars += starLayer(dx, dy, dz, 48f,0.46f, 0.65f, rgb, seed + 101L);
                stars += starLayer(dx, dy, dz, 100f, 0.50f, 0.38f, rgb, seed + 202L);

                px[row + x] = NoiseUtil.toRGBA(rgb[0], rgb[1], rgb[2], NoiseUtil.clamp01(stars));
            }
        });

        return BakingMath.blit(px, res, res);
    }

    private static float starLayer(float dx, float dy, float dz, float cellScale, float density, float brightness, float[] rgb, long seed)
    {
        float px = dx * cellScale, py = dy * cellScale, pz = dz * cellScale;
        int cx = (int) Math.floor(px), cy = (int) Math.floor(py), cz = (int) Math.floor(pz);
        float fx = px - cx, fy = py - cy, fz = pz - cz;

        float texel = 1.2f / cellScale;
        float total = 0f;

        for (int oz = -1; oz <= 1; oz++)
            for (int oy = -1; oy <= 1; oy++)
                for (int ox = -1; ox <= 1; ox++)
                {
                    int gx = cx + ox, gy = cy + oy, gz = cz + oz;

                    float r0 = NoiseUtil.pseudoRandom3(gx, gy, gz, seed);
                    if (r0 > density) continue;

                    float r1 = NoiseUtil.pseudoRandom3(gx, gy, gz, seed + 1L);
                    float r2 = NoiseUtil.pseudoRandom3(gx, gy, gz, seed + 2L);

                    float jx = NoiseUtil.pseudoRandom3(gx, gy, gz, seed + 17L);
                    float jy = NoiseUtil.pseudoRandom3(gx, gy, gz, seed + 18L);
                    float jz = NoiseUtil.pseudoRandom3(gx, gy, gz, seed + 19L);

                    float ddx = ox + jx - fx, ddy = oy + jy - fy, ddz = oz + jz - fz;
                    float d = (float) Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);

                    float size = Math.max(NoiseUtil.lerp(0.016f, 0.055f, r1 * r1), texel * 1.1f);
                    float core = (float) Math.pow(NoiseUtil.clamp01(1f - d / size), 2.3);
                    float glow = (float) Math.pow(NoiseUtil.clamp01(1f - d / (size * 2f)), 3.5) * r1 * 0.22f;

                    float amount = (core + glow) * brightness * NoiseUtil.lerp(0.45f, 1.0f, r1);
                    if (amount <= 0f) continue;

                    float sr = NoiseUtil.lerp(NoiseUtil.lerp(0.58f, 0.86f, r2), 1f, 0.25f);
                    float sg = NoiseUtil.lerp(NoiseUtil.lerp(0.85f, 0.97f, r2), 1f, 0.25f);
                    float sb = 1.0f;

                    rgb[0] += sr * amount;
                    rgb[1] += sg * amount;
                    rgb[2] += sb * amount;
                    total += amount;
                }

        return total;
    }

    private static float fbm(float x, float y, float z, long seed)
    {
        return NoiseUtil.fbm3(x, y, z, seed, 5, 0.5f);
    }
}
