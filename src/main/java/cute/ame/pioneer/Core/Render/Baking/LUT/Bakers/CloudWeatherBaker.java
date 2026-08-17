package cute.ame.pioneer.Core.Render.Baking.LUT.Bakers;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.Core.Render.Baking.LUT.BakingMath;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTBaker;
import cute.ame.pioneer.Core.Render.Baking.LUT.LUTParams;
import cute.ame.pioneer.Core.Render.Helper.NoiseUtil;

import java.util.stream.IntStream;

public final class CloudWeatherBaker implements LUTBaker
{
    private static final float FRONT_SCALE = 1.5f;
    private static final float CELL_SCALE = 7.0f;
    private static final float TYPE_SCALE = 3.4f;
    private static final float TYPE_OFF = 19.2f;

    private static final float CELL_CONTRAST = 1.85f;
    private static final float FRONT_WEIGHT = 0.55f;
    private static final float BAND_DEPTH = 0.14f;

    @Override
    public NativeImage bake(LUTParams params)
    {
        final int res = params.resolution();
        final long seed = params.seed();

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
                final float dx = dir[0], dy = dir[1], dz = dir[2];

                float fronts = NoiseUtil.fbmP(dx * FRONT_SCALE, dy * FRONT_SCALE, dz * FRONT_SCALE, seed + 0x4F20L, 3, 0.5f);
                final float w = fronts * 0.25f;
                float cells = NoiseUtil.fbmP((dx + w) * CELL_SCALE, (dy + w) * CELL_SCALE, (dz + w) * CELL_SCALE, seed, 5, 0.52f);

                float band = BAND_DEPTH * (float) Math.cos(dy * 9.0f);
                float macro = NoiseUtil.clamp01(cells * CELL_CONTRAST * 0.5f + 0.5f + fronts * FRONT_WEIGHT * 0.5f + band);
                float type = BakingMath.contrast(NoiseUtil.fbmP01(dx * TYPE_SCALE + TYPE_OFF, dy * TYPE_SCALE + TYPE_OFF, dz * TYPE_SCALE + TYPE_OFF, seed + 977L, 3, 0.5f), 1.7f);

                px[row + x] = NoiseUtil.toRGBA(macro, type, 0f, 1f);
            }
        });

        return BakingMath.blit(px, res, res);
    }
}