package cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.BakingMath;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTBaker;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTParams;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil;

import java.util.stream.IntStream;

public final class CloudVolumeBaker implements LUTBaker
{
    public static final int BASE_FREQ = 4;
    private static final int OCTAVES = 4;

    @Override
    public NativeImage bake(LUTParams params)
    {
        final int res = params.resolution();
        final int tiles = (int) Math.ceil(Math.sqrt(res));
        final int atlas = res * tiles;
        final long seed = params.seed();

        final int[] px = new int[atlas * atlas];
        final float inv = 1f / res;

        IntStream.range(0, res).parallel().forEach(z ->
        {
            int tileX = (z % tiles) * res;
            int tileY = (z / tiles) * res;
            float w = z * inv;

            for (int y = 0; y < res; y++)
            {
                float v = y * inv;
                int row = (tileY + y) * atlas + tileX;

                for (int x = 0; x < res; x++)
                {
                    float u = x * inv;

                    float base = BakingMath.fbmPeriodic(u, v, w, BASE_FREQ, OCTAVES, seed);
                    float e0 = 1f - BakingMath.worleyPeriodic(u, v, w, 4, seed + 0x51ED2701L);
                    float e1 = 1f - BakingMath.worleyPeriodic(u, v, w, 8, seed + 0x9E37B31CL);
                    float e2 = 1f - BakingMath.worleyPeriodic(u, v, w, 16, seed + 0xC2B2AE35L);

                    px[row + x] = NoiseUtil.toRGBA(base, e0, e1, e2);
                }
            }
        });

        return BakingMath.blit(px, atlas, atlas);
    }
}
