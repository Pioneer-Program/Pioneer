package cute.ame.pioneer.Core.Render.Baking.LUT.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.Core.Render.Baking.LUT.BakingMath;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTBaker;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTParams;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;

import java.util.stream.IntStream;

public final class CloudVolumeBaker implements LUTBaker
{
    public static final int BASE_FREQ = 4;
    private static final int SHAPE_OCTAVES = 3;

    @Override
    public NativeImage bake(LUTParams params)
    {
        final int res = params.resolution();
        final int tiles = (int) Math.ceil(Math.sqrt(res));
        final int atlas = res * tiles;
        final int slice = res * res;
        final long seed = params.seed();

        final float[] shape = new float[slice * res];
        final float[] detail = new float[slice * res];
        final float inv = 1f / res;

        IntStream.range(0, res).parallel().forEach(z ->
        {
            float w = z * inv;
            int i = z * slice;

            for (int y = 0; y < res; y++)
            {
                float v = y * inv;

                for (int x = 0; x < res; x++, i++)
                {
                    float u = x * inv;

                    float perlin = NoiseUtil.clamp01(BakingMath.perlinFbmPeriodic(u, v, w, BASE_FREQ, SHAPE_OCTAVES, seed) * 0.5f + 0.5f);
                    float billow = BakingMath.worleyFbmPeriodic(u, v, w, BASE_FREQ, seed + 0x2545F491L);

                    shape[i] = billow + perlin * (1f - billow);
                    detail[i] = billow;
                }
            }
        });

        BakingMath.equalize(shape);
        BakingMath.equalize(detail);

        final int[] px = new int[atlas * atlas];

        IntStream.range(0, res).parallel().forEach(z ->
        {
            int row = ((z / tiles) * res) * atlas + (z % tiles) * res;
            int i0 = z * slice;
            int i1 = ((z + 1) % res) * slice;

            for (int y = 0; y < res; y++, row += atlas)
                for (int x = 0; x < res; x++, i0++, i1++)
                    px[row + x] = NoiseUtil.toRGBA(shape[i0], shape[i1], detail[i0], detail[i1]);
        });

        return BakingMath.blit(px, atlas, atlas);
    }
}
