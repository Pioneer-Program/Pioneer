package cute.ame.pioneer.Planet.Arid.Worldgen.Feature;

import com.mojang.serialization.Codec;
import cute.ame.pioneer.Registrie.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class AridStrataFeature extends Feature<NoneFeatureConfiguration>
{
  public AridStrataFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }
  private static final BlockState[] STRATA = null;

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featurePlaceContext)
  {
    return place(featurePlaceContext.config(), featurePlaceContext.level(), featurePlaceContext.chunkGenerator(), featurePlaceContext.random(), featurePlaceContext.origin());
  }

  @Override
  public boolean place(NoneFeatureConfiguration config, WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
    BlockState[] strata =
    {
        ModBlocks.ARID_STRATA_RED.get().defaultBlockState(),
        ModBlocks.ARID_STRATA_ORANGE.get().defaultBlockState(),
        ModBlocks.ARID_STRATA_WHITE.get().defaultBlockState(),
        ModBlocks.ARID_STRATA_MAROON.get().defaultBlockState()
    };

    BlockState chosen = strata[random.nextInt(strata.length)];
    int width = 8 + random.nextInt(9);
    int depth = 2 + random.nextInt(3);
    int halfWidth = width / 2;

    boolean placed = false;
    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

    for(int dx = -halfWidth; dx <= halfWidth; ++dx)
      for (int dy = 0; dy < depth; ++dy)
        for (int dz = -halfWidth; dz <= halfWidth; ++dz)
        {
          mpos.setWithOffset(origin, dx, dy, dz);
          BlockState existing = level.getBlockState(mpos);

          if (existing.is(ModBlocks.ARID_ROCK.get()) || existing.is(ModBlocks.ARID_ROCK_CRACKED.get()))
          {
            level.setBlock(mpos, chosen, 2);
            placed = true;
          }
        }

    return placed;
  }
}
