package cute.ame.pioneer.Planet.Arid.Worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

public final class AridBiomeSource extends BiomeSource
{
  public static final int DUST = 0;
  public static final int CANYON = 1;
  public static final int CRYSTAL = 2;
  public static final int CRATER = 3;

  private static final float FREQ_MAIN = 0.00040f;
  private static final float FREQ_WARP = 0.00090f;

  public static final MapCodec<AridBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(Biome.LIST_CODEC.fieldOf("biomes").forGetter(b -> b.biomeList)).apply(instance, AridBiomeSource::new)
  );

  private final HolderSet<Biome> biomeList;

  public AridBiomeSource(HolderSet<Biome> biomeList) { this.biomeList = biomeList; }

  @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
  @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return biomeList.stream(); }

  @Override
  public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler)
  {
    int idx = getBiomeIndex(x << 2, z << 2);
    if (idx >= biomeList.size()) idx = DUST;
    return biomeList.get(idx);
  }

  public int getBiomeIndex(int x, int z)
  {
    float wx = AridNoise.fbm(x * FREQ_WARP + 1000, z * FREQ_WARP, 50L, 3, 0.55f) * 400f;
    float wz = AridNoise.fbm(x * FREQ_WARP, z * FREQ_WARP + 2000, 150L, 3, 0.55f) * 400f;
    float bx = x + wx, bz = z + wz;

    float a = AridNoise.fbm(bx * FREQ_MAIN, bz * FREQ_MAIN, 1L, 5, 0.52f) * 2f - 1f;
    float b = AridNoise.fbm(bx * FREQ_MAIN + 500, bz * FREQ_MAIN + 300, 300L, 4, 0.50f) * 2f - 1f;

    if (a > 0.22f) return CANYON;
    else if (a < -0.28f && b < -0.12f) return CRYSTAL;
    else return DUST;
  }

  public HolderSet<Biome> getBiomeList() { return biomeList; }
}