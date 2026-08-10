package cute.ame.pioneer.SkyPlanet.Dimension;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;
import java.util.List;

public final class SpaceDimensionChunkGenerator extends ChunkGenerator
{
    public static final MapCodec<SpaceDimensionChunkGenerator> CODEC = BiomeSource.CODEC.fieldOf("biome_source").xmap(SpaceDimensionChunkGenerator::new, ChunkGenerator::getBiomeSource);

    public SpaceDimensionChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving carving)
    {

    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk)
    {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion level)
    {}

    @Override
    public int getGenDepth()
    {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess)
    {
        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getSeaLevel()
    {
        return 0;
    }

    @Override
    public int getMinY()
    {
        return -512;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, net.minecraft.world.level.LevelHeightAccessor level, RandomState random)
    {
        return getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int i1, LevelHeightAccessor levelHeightAccessor, RandomState randomState)
    {
        return new NoiseColumn(getMinY(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, net.minecraft.core.BlockPos pos)
    {

    }
}