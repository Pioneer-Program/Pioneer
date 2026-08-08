package cute.ame.auralithpioneerinitiative.Registrie;

import com.mojang.serialization.MapCodec;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen.AridBiomeSource;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen.AridChunkGenerator;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen.Feature.AridStrataFeature;
import cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen.Feature.CrystalSpikeFeature;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Dimension.SpaceDimensionChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen
{
  public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Auralithpioneerinitiative.MODID);
  public static final DeferredHolder<Feature<?>, AridStrataFeature> ARID_STRATA = FEATURES.register("arid_strata", () -> new AridStrataFeature(NoneFeatureConfiguration.CODEC));
  public static final DeferredHolder<Feature<?>, CrystalSpikeFeature> CRYSTAL_SPIKE = FEATURES.register("crystal_spike", () -> new CrystalSpikeFeature(NoneFeatureConfiguration.CODEC));

  public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, Auralithpioneerinitiative.MODID);
  public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<AridBiomeSource>> ARID_BIOME_SOURCE = BIOME_SOURCES.register("arid_biome_source", () -> AridBiomeSource.CODEC);

  public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, Auralithpioneerinitiative.MODID);
  public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<AridChunkGenerator>> ARID_GENERATOR = CHUNK_GENERATORS.register("arid_generator", () -> AridChunkGenerator.CODEC);
  public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<SpaceDimensionChunkGenerator>> SPACE_GENERATOR = CHUNK_GENERATORS.register("space", () -> SpaceDimensionChunkGenerator.CODEC);
}