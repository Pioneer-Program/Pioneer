package cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen.Feature;

import com.mojang.serialization.Codec;
import cute.ame.auralithpioneerinitiative.Registrie.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class CrystalSpikeFeature extends Feature<NoneFeatureConfiguration>
{
  private static final BlockState LARGE  = ModBlocks.ARID_CRYSTAL_LARGE .get().defaultBlockState();
  private static final BlockState MEDIUM = ModBlocks.ARID_CRYSTAL_MEDIUM.get().defaultBlockState();
  private static final BlockState SMALL  = ModBlocks.ARID_CRYSTAL_SMALL .get().defaultBlockState();

  private static final int SPIKE_MIN = 3;
  private static final int SPIKE_MAX = 9;
  private static final int SPREAD_XZ = 5;

  private static final int BASE_MIN = 1;
  private static final int BASE_MAX = 4;
  private static final int MID_MIN = 1;
  private static final int MID_MAX = 5;
  private static final int TIP_MIN = 1;
  private static final int TIP_MAX = 3;

  public CrystalSpikeFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
  {
    WorldGenLevel level  = context.level();
    RandomSource random = context.random();
    BlockPos origin = context.origin();

    int spikeCount = SPIKE_MIN + random.nextInt(SPIKE_MAX - SPIKE_MIN);
    boolean placed = false;

    for (int i = 0; i < spikeCount; i++)
    {
      int ox = origin.getX() + random.nextInt(SPREAD_XZ * 2 + 1) - SPREAD_XZ;
      int oz = origin.getZ() + random.nextInt(SPREAD_XZ * 2 + 1) - SPREAD_XZ;
      BlockPos ground = findGround(level, ox, origin.getY(), oz);
      if (ground == null) continue;

      placed |= placeSpike(level, random, ground);
    }

    return placed;
  }

  private static BlockPos findGround(WorldGenLevel level, int x, int startY, int z)
  {
    int searchTop = startY + 8;
    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(x, searchTop, z);

    for (int y = searchTop; y >= startY - 16; y--)
    {
      mpos.setY(y);
      BlockState below = level.getBlockState(mpos.below());
      BlockState here  = level.getBlockState(mpos);

      if (below.isSolid() && here.isAir()) return mpos.immutable();
    }
    return null;
  }

  private static boolean placeSpike(WorldGenLevel level, RandomSource random, BlockPos base)
  {
    int baseHeight = BASE_MIN + random.nextInt(BASE_MAX - BASE_MIN);
    int midHeight = MID_MIN + random.nextInt(MID_MAX - MID_MIN);
    int tipHeight = TIP_MIN + random.nextInt(TIP_MAX - TIP_MIN);

    BlockPos.MutableBlockPos mpos = base.mutable();
    boolean placed = false;

    for (int i = 0; i < baseHeight; i++)
    {
      if (canPlace(level, mpos)) { level.setBlock(mpos, LARGE, 2); placed = true; }
      mpos.move(0, 1, 0);
    }

    for (int i = 0; i < midHeight; i++)
    {
      if (canPlace(level, mpos)) { level.setBlock(mpos, MEDIUM, 2); placed = true; }
      mpos.move(0, 1, 0);
    }

    for (int i = 0; i < tipHeight; i++)
    {
      if (canPlace(level, mpos)) { level.setBlock(mpos, SMALL, 2); placed = true; }
      mpos.move(0, 1, 0);
    }

    return placed;
  }

  private static boolean canPlace(WorldGenLevel level, BlockPos pos)
  {
    BlockState existing = level.getBlockState(pos);
    return existing.isAir() || existing.canBeReplaced();
  }
}