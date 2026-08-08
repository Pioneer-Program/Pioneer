package cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.BakingMath;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTBaker;
import cute.ame.auralithpioneerinitiative.Core.Render.Baking.LUT.LUTParams;
import cute.ame.auralithpioneerinitiative.Core.Render.Helper.NoiseUtil;

import java.util.stream.IntStream;

public final class CloudWeatherBaker implements LUTBaker
{
    private static final float SCALE = 1.44f;
    private static final float TYPE_OFF = 19.2f;
    private static final float CONTRAST = 2.4f;

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
            float v = y * inv;
            int row = y * res;

            for (int x = 0; x < res; x++)
            {
                BakingMath.octaDecode(x * inv, v, dir);
                float dx = dir[0] * SCALE, dy = dir[1] * SCALE, dz = dir[2] * SCALE;
                float macro = BakingMath.contrast(NoiseUtil.fbm3(dx, dy, dz, seed, 3, 0.5f), CONTRAST);
                float type = BakingMath.contrast(NoiseUtil.fbm3(dx * 1.7f + TYPE_OFF, dy * 1.7f + TYPE_OFF, dz * 1.7f + TYPE_OFF, seed + 977L, 2, 0.5f), CONTRAST);

                px[row + x] = NoiseUtil.toRGBA(macro, type, 0f, 1f);
            }
        });

        return BakingMath.blit(px, res, res);
    }
}
