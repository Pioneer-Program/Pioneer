package cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.auralithpioneerinitiative.Registrie.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AridChunkGenerator extends ChunkGenerator
{
  public static final MapCodec<AridChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
          BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
          com.mojang.serialization.Codec.LONG.fieldOf("seed").forGetter(g -> g.seed)
      ).apply(instance, AridChunkGenerator::new)
  );

  private static final int MIN_Y = -64;
  private static final int SEA_LEVEL = -63;
  private static final int GEN_DEPTH = 384;
  private static final int BASE_Y = 72;

  private static final int CRATER_CELL = 512;
  private static final int CRATER_CHANCE = 4;
  private static final int CRATER_MIN_R = 60;
  private static final int CRATER_MAX_R = 220;
  private static final float CRATER_MAX_DEPTH = 36f;

  private static final int PLATEAU_Y = 85;
  private static final int CANYON_FLOOR = 42;
  private static final int BLEND_RADIUS = 96;
  private static final int BLEND_STEP = 16;

  private static final int CAVE_CELL = 192;
  private static final int CAVE_PROB = 2;
  private static final int SURFACE_GAP = 16;
  private static final int CAVE_MAX_RH = 90;

  private final BiomeSource biomeSource;
  private final long seed;

  public AridChunkGenerator(BiomeSource biomeSource, long seed)
  {
    super(biomeSource);
    this.biomeSource = biomeSource;
    this.seed = seed;
  }

  @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
  @Override public int getSeaLevel() { return SEA_LEVEL; }
  @Override public int getGenDepth() { return GEN_DEPTH; }
  @Override public int getMinY() { return MIN_Y; }

  @Override
  public void addDebugScreenInfo(List<String> info, RandomState state, BlockPos pos)
  {
    int biome = getBiomeType(pos.getX(), pos.getZ());
    String[] names = {"Dust Desert","Canyon Maze","Crystal Caverns","Impact Crater"};
    info.add("[Arid] Biome: " + names[Math.min(biome, names.length - 1)]);
    info.add("[Arid] SurfaceY: " + getSurfaceHeight(pos.getX(), pos.getZ()));
    info.add("[Arid] CanyonVal: " + String.format("%.6f", getCanyonValue(pos.getX(), pos.getZ())));
  }

  @Override
  public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState state)
  { return getSurfaceHeight(x, z); }

  @Override
  public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState state)
  {
    int sy = getSurfaceHeight(x, z);
    int total = level.getHeight();
    BlockState[] col = new BlockState[total];
    for (int i = 0; i < total; i++)
    {
      int wy = level.getMinBuildHeight() + i;
      if (wy < MIN_Y + 1) col[i] = Blocks.BEDROCK.defaultBlockState();
      else if (wy < sy) col[i] = ModBlocks.ARID_ROCK.get().defaultBlockState();
      else col[i] = Blocks.AIR.defaultBlockState();
    }
    return new NoiseColumn(level.getMinBuildHeight(), col);
  }

  public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk)
  {
    return CompletableFuture.supplyAsync(() ->
    {
      int minCX = chunk.getPos().getMinBlockX();
      int minCZ = chunk.getPos().getMinBlockZ();
      BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
      BlockState rock = ModBlocks.ARID_ROCK.get().defaultBlockState();
      BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

      for (int lx = 0; lx < 16; lx++)
      {
        int wx = minCX + lx;
        for (int lz = 0; lz < 16; lz++)
        {
          int wz = minCZ + lz;
          int sy = getSurfaceHeight(wx, wz);
          for (int y = MIN_Y; y < sy; y++)
          {
            mpos.set(wx, y, wz);
            boolean bed = y <= MIN_Y + 4 && y < MIN_Y + (int)(AridNoise.hash2(wx, wz, seed) * 5);
            chunk.setBlockState(mpos, bed ? bedrock : rock, false);
          }
        }
      }
      return chunk;
    });
  }

  @Override
  public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk)
  {
    int minCX = chunk.getPos().getMinBlockX();
    int minCZ = chunk.getPos().getMinBlockZ();
    BlockState dust = ModBlocks.ARID_DUST.get().defaultBlockState();
    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

    for (int lx = 0; lx < 16; lx++)
    {
      int wx = minCX + lx;
      for (int lz = 0; lz < 16; lz++)
      {
        int wz = minCZ + lz;
        int sy = getSurfaceHeight(wx, wz);
        int biome = getBiomeType(wx, wz);

        int dustDepth = (biome == AridBiomeSource.DUST || biome == AridBiomeSource.CRATER) ? 3 : 1;
        for (int d = 0; d < dustDepth; d++)
        {
          mpos.set(wx, sy - 1 - d, wz);
          if (!chunk.getBlockState(mpos).isAir()) chunk.setBlockState(mpos, dust, false);
        }

        if (biome == AridBiomeSource.CANYON) placeStrataColumn(chunk, wx, wz, sy, mpos);
      }
    }
  }

  @Override
  public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step)
  {
    if (step != GenerationStep.Carving.AIR) return;

    int minCX = chunk.getPos().getMinBlockX();
    int minCZ = chunk.getPos().getMinBlockZ();

    int margin = CAVE_CELL + CAVE_MAX_RH;
    int cellX0 = Math.floorDiv(minCX - margin, CAVE_CELL);
    int cellX1 = Math.floorDiv(minCX + 15 + margin,  CAVE_CELL);
    int cellZ0 = Math.floorDiv(minCZ - margin, CAVE_CELL);
    int cellZ1 = Math.floorDiv(minCZ + 15 + margin,  CAVE_CELL);

    for (int cx = cellX0; cx <= cellX1; cx++)
      for (int cz = cellZ0; cz <= cellZ1; cz++)
        carveCaveSystem(chunk, cx, cz);
  }

  private void carveCaveSystem(ChunkAccess chunk, int cellX, int cellZ)
  {
    long h0 = hashCell(cellX, cellZ, seed ^ 0xACABABEDEADBEEFL);
    if (Long.remainderUnsigned(h0, CAVE_PROB) != 0) return;

    long hPos = hashCell(cellX, cellZ, seed + 0x1L);
    int worldX = cellX * CAVE_CELL + CAVE_CELL / 2 + (int)((hPos & 0x3FL) - 32L);
    int worldZ = cellZ * CAVE_CELL + CAVE_CELL / 2 + (int)(((hPos >>> 8) & 0x3FL) - 32L);
    if (getBiomeType(worldX, worldZ) == AridBiomeSource.CANYON) return;

    int surfaceY = getSurfaceHeight(worldX, worldZ);
    int maxCaveY = surfaceY - SURFACE_GAP;
    float yn = AridNoise.fbm(cellX * 0.38f, cellZ * 0.38f, seed + 3L, 3, 0.50f);
    int caveY = 8 + (int)(yn * 32f);
    caveY = Math.min(caveY, maxCaveY - 38);
    if (caveY < MIN_Y + 12) return;

    long hDim = hashCell(cellX, cellZ, seed + 4L);
    int rH = 45 + (int)(hDim & 0x1FL);
    int rV = 22 + (int)((hDim >>> 5) & 0xFL );
    carveNoiseEllipsoid(chunk, worldX, caveY, worldZ, rH, rV, seed + 10L, maxCaveY);

    int subCount = 4 + (int)(hashCell(cellX, cellZ, seed + 5L) & 3L);
    for (int i = 0; i < subCount; i++)
    {
      long hs = hashCell(cellX * 1_000L + i, cellZ * 1_000L + i, seed + 6L + i);

      double angle = ((hs & 0xFFFFL) / 65535.0) * Math.PI * 2.0;
      int dist = rH + 30 + (int)((hs >>> 16) & 0x3FL);
      int sx = worldX + (int)(Math.cos(angle) * dist);
      int sz = worldZ + (int)(Math.sin(angle) * dist);
      int sdy = (int)(((hs >>> 22) & 0x1FL) - 12L);
      int sy = Math.min(caveY + sdy, maxCaveY - 24);
      if (sy < MIN_Y + 8) continue;

      int sr = 20 + (int)((hs >>> 27) & 0x1FL);
      int srV = 12 + (int)((hs >>> 32) & 0xFL );

      carveNoiseEllipsoid(chunk, sx, sy, sz, sr, srV, seed + 20L + i, maxCaveY);
      int tubeR = 5 + (int)((hs >>> 36) & 0x3L);
      carveWorm(chunk, worldX, caveY, worldZ, sx, sy, sz, tubeR, seed + 50L + i, maxCaveY);
    }

    int rampCount = 2 + (int)(hashCell(cellX, cellZ, seed + 7L) & 1L);
    for (int i = 0; i < rampCount; i++)
    {
      long hr = hashCell(cellX * 700L + i, cellZ * 700L + i, seed + 8L + i);

      double rampAngle = ((hr & 0xFFFFL) / 65535.0) * Math.PI * 2.0;
      int rampLen = 55 + (int)((hr >>> 16) & 0x3FL);
      int rampDY = 18 + (int)((hr >>> 22) & 0x1FL);
      if (i % 2 == 1) rampDY = -rampDY;

      int ex = worldX + (int)(Math.cos(rampAngle) * rampLen);
      int ez = worldZ + (int)(Math.sin(rampAngle) * rampLen);
      int ey = Math.max(MIN_Y + 6, Math.min(caveY + rampDY, maxCaveY - 5));

      carveWorm(chunk, worldX, caveY, worldZ, ex, ey, ez, 7, seed + 80L + i, maxCaveY);
    }

    int alcoveCount = 3 + (int)(hashCell(cellX, cellZ, seed + 9L) & 3L);
    for (int i = 0; i < alcoveCount; i++)
    {
      long ha = hashCell(cellX * 300L + i, cellZ * 300L + i, seed + 100L + i);
      double aAngle = ((ha & 0xFFFFL) / 65535.0) * Math.PI * 2.0;
      int aDist = rH / 2 + (int)((ha >>> 16) & 0x1FL);
      int ax = worldX + (int)(Math.cos(aAngle) * aDist);
      int az = worldZ + (int)(Math.sin(aAngle) * aDist);
      int ay = caveY + (int)(((ha >>> 22) & 0x1FL) - 10L);
      ay = Math.min(ay, maxCaveY - 10);
      if (ay < MIN_Y + 5) continue;

      int ar = 10 + (int)((ha >>> 27) & 0xFL);
      int arV = 7 + (int)((ha >>> 31) & 0x7L);
      carveNoiseEllipsoid(chunk, ax, ay, az, ar, arV, seed + 200L + i, maxCaveY);
    }
  }

  private void carveNoiseEllipsoid(ChunkAccess chunk, int ox, int oy, int oz, int rH, int rV, long noiseSeed, int maxY)
  {
    int minCX = chunk.getPos().getMinBlockX();
    int minCZ = chunk.getPos().getMinBlockZ();
    float rH2 = (float)(rH * rH);
    float rV2 = (float)(rV * rV);
    float freq = 0.022f;
    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

    for (int dx = -rH; dx <= rH; dx++)
    {
      int bx = ox + dx;
      if (bx < minCX || bx >= minCX + 16) continue;

      for (int dz = -rH; dz <= rH; dz++)
      {
        int bz = oz + dz;
        if (bz < minCZ || bz >= minCZ + 16) continue;

        float hFrac = (dx * dx + dz * dz) / rH2;
        if (hFrac >= 1.15f) continue;

        float wallWarp = AridNoise.fbm(bx * freq, bz * freq, noiseSeed, 4, 0.55f) * 2f - 1f;
        float warpedH = hFrac - wallWarp * 0.28f;
        if (warpedH >= 1.0f) continue;

        int baseMaxDY = (int) Math.sqrt(Math.max(0f, rV2 * (1f - warpedH)));

        float ceilWarp = AridNoise.fbm(bx * freq * 1.5f + 73f, bz * freq * 1.5f + 19f, noiseSeed + 1L, 3, 0.52f) * 2f - 1f;
        float floorWarp = AridNoise.fbm(bx * freq * 1.2f + 37f, bz * freq * 1.2f + 91f, noiseSeed + 2L, 2, 0.48f) * 2f - 1f;

        int topDY = baseMaxDY + (int)(ceilWarp * rV * 0.30f);
        int bottomDY = -baseMaxDY + (int)(floorWarp * rV * 0.12f);

        for (int dy = bottomDY; dy <= topDY; dy++)
        {
          int by = oy + dy;
          if (by < MIN_Y + 2 || by > maxY) continue;
          mpos.set(bx, by, bz);
          if (!chunk.getBlockState(mpos).isAir()) chunk.setBlockState(mpos, Blocks.AIR.defaultBlockState(), false);
        }
      }
    }
  }

  private void carveWorm(ChunkAccess chunk, int x0, int y0, int z0, int x1, int y1, int z1, int radius, long wormSeed, int maxY)
  {
    int minCX = chunk.getPos().getMinBlockX();
    int minCZ = chunk.getPos().getMinBlockZ();
    float r2 = (float)(radius * radius);
    double dist = Math.sqrt((double)(x1-x0)*(x1-x0) + (double)(y1-y0)*(y1-y0) + (double)(z1-z0)*(z1-z0));
    int steps = Math.max(2, (int)(dist / 3.0));
    float amplitude = radius * 1.5f;
    float freqT = 2.8f;
    BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

    for (int s = 0; s <= steps; s++)
    {
      float t = (float) s / steps;
      float nx = (AridNoise.fbm(t * freqT, 0.0f, wormSeed, 3, 0.52f) * 2f - 1f) * amplitude;
      float nz = (AridNoise.fbm(t * freqT + 100f,  0.0f, wormSeed + 1L, 3, 0.52f) * 2f - 1f) * amplitude;
      float ny = (AridNoise.fbm(t * freqT + 200f,  0.0f, wormSeed + 2L, 2, 0.48f) * 2f - 1f) * amplitude * 0.25f;
      int cx = Math.round(x0 + t * (x1 - x0) + nx);
      int cy = Math.round(y0 + t * (y1 - y0) + ny);
      int cz = Math.round(z0 + t * (z1 - z0) + nz);
      if (cy < MIN_Y + 2 || cy > maxY) continue;

      for (int ox2 = -radius; ox2 <= radius; ox2++)
      {
        int bx = cx + ox2;
        if (bx < minCX || bx >= minCX + 16) continue;
        for (int oz2 = -radius; oz2 <= radius; oz2++)
        {
          int bz = cz + oz2;
          if (bz < minCZ || bz >= minCZ + 16) continue;
          for (int oy2 = -radius; oy2 <= radius; oy2++)
          {
            if (ox2*ox2 + oy2*oy2 + oz2*oz2 > r2) continue;
            int by = cy + oy2;
            if (by < MIN_Y + 2 || by > maxY) continue;
            mpos.set(bx, by, bz);
            if (!chunk.getBlockState(mpos).isAir()) chunk.setBlockState(mpos, Blocks.AIR.defaultBlockState(), false);
          }
        }
      }
    }
  }

  @Override public void spawnOriginalMobs(WorldGenRegion region) {}

  public int getSurfaceHeight(int x, int z)
  {
    int base = blendedBiomeHeight(x, z);
    int craterDep = getCraterDepression(x, z);
    return Math.max(MIN_Y + 5, base - craterDep);
  }

  private int blendedBiomeHeight(int x, int z)
  {
    if (!(biomeSource instanceof AridBiomeSource abs)) return rawBiomeHeight(AridBiomeSource.DUST, x, z);
    double totalW = 0.0, totalH = 0.0;
    double sigma2 = (BLEND_RADIUS * 0.5) * (BLEND_RADIUS * 0.5);

    for (int ox = -BLEND_RADIUS; ox <= BLEND_RADIUS; ox += BLEND_STEP)
      for (int oz = -BLEND_RADIUS; oz <= BLEND_RADIUS; oz += BLEND_STEP)
      {
        int sx = x + ox, sz = z + oz;
        int biome = abs.getBiomeIndex(sx, sz);
        double dist2 = (double) ox * ox + (double) oz * oz;
        double w = Math.exp(-dist2 / (2.0 * sigma2));
        totalH += rawBiomeHeight(biome, sx, sz) * w;
        totalW += w;
      }

    return totalW > 0.0 ? (int) Math.round(totalH / totalW) : BASE_Y;
  }

  private int rawBiomeHeight(int biome, int x, int z)
  {
    return switch (biome)
    {
      case AridBiomeSource.CANYON -> canyonH(x, z);
      case AridBiomeSource.CRYSTAL -> crystalH(x, z);
      default -> dustH(x, z);
    };
  }

  private int dustH(int x, int z)
  {
    float dune = (float)(Math.sin(x * 0.045) * 5.0 + Math.sin(z * 0.038 + x * 0.018) * 4.0);
    float fbm = AridNoise.fbm(x * 0.025f, z * 0.025f, seed + 2L, 5, 0.52f) * 10f - 5f;
    float detail = AridNoise.fbm(x * 0.10f,  z * 0.10f,  seed + 20L, 3, 0.45f) * 3f - 1.5f;
    return BASE_Y + (int)(dune + fbm + detail);
  }

  private int canyonH(int x, int z)
  {
    float fx = x * 0.0055f, fz = z * 0.0055f;
    float dwx = AridNoise.fbm(fx + 3.7f, fz + 1.3f, seed + 30L, 3, 0.5f) * 1.8f;
    float dwz = AridNoise.fbm(fx + 8.1f, fz + 5.2f, seed + 31L, 3, 0.5f) * 1.8f;
    float wx = fx + dwx, wz = fz + dwz;
    float main = AridNoise.fbm(wx, wz, seed + 3L, 4, 0.52f);
    float mainDist = Math.abs(main - 0.50f) * 2f;
    float sec = AridNoise.fbm(wx * 2.1f + 15, wz * 2.1f + 22, seed + 40L, 3, 0.50f);
    float secDist = Math.abs(sec - 0.50f) * 2f;
    float canalDist = Math.min(mainDist, secDist * 0.75f + 0.1f);
    float platVar = AridNoise.fbm(x * 0.018f, z * 0.018f, seed + 4L, 3, 0.48f) * 9f - 4.5f;
    int plateau = PLATEAU_Y + (int) platVar;

    if (canalDist < 0.32f)
    {
      float t = 1f - canalDist / 0.32f;
      t = t * t * (3f - 2f * t);
      float floorVar = AridNoise.fbm(x * 0.04f, z * 0.04f, seed + 50L, 3, 0.45f) * 6f - 3f;
      return (int) AridNoise.lerp(plateau, CANYON_FLOOR + (int) floorVar, t);
    }
    else if (canalDist < 0.48f)
    {
      float t = (canalDist - 0.32f) / 0.16f;
      t = t * t * (3f - 2f * t);
      float rimBump = AridNoise.fbm(x * 0.06f, z * 0.06f, seed + 60L, 2, 0.5f) * 4f;
      return (int) AridNoise.lerp(plateau, plateau + 3 + (int) rimBump, 1f - t);
    }

    return plateau;
  }

  private int crystalH(int x, int z)
  {
    float n = AridNoise.fbm(x * 0.030f, z * 0.030f, seed + 5L, 5, 0.50f);
    float detail = AridNoise.fbm(x * 0.08f,  z * 0.08f,  seed + 51L, 3, 0.45f) * 4f - 2f;
    return 55 + (int)(n * 14f - 7f) + (int) detail;
  }

  private int getCraterDepression(int x, int z)
  {
    int cellX = Math.floorDiv(x, CRATER_CELL);
    int cellZ = Math.floorDiv(z, CRATER_CELL);
    double maxDep = 0.0;

    for (int dcx = -1; dcx <= 1; dcx++)
      for (int dcz = -1; dcz <= 1; dcz++)
      {
        int cx = cellX + dcx, cz = cellZ + dcz;
        long h = hashCell(cx, cz, seed);
        if ((h & 0xFF) >= (256 / CRATER_CHANCE)) continue;

        double jX = ((hashCell(cx, cz, seed + 1) & 0xFFFF) / 65535.0 - 0.5);
        double jZ = ((hashCell(cx, cz, seed + 2) & 0xFFFF) / 65535.0 - 0.5);
        double crX = cx * CRATER_CELL + CRATER_CELL * 0.5 + jX * CRATER_CELL * 0.8;
        double crZ = cz * CRATER_CELL + CRATER_CELL * 0.5 + jZ * CRATER_CELL * 0.8;
        int r = CRATER_MIN_R + (int)((hashCell(cx, cz, seed + 3) & 0xFF) / 255.0 * (CRATER_MAX_R - CRATER_MIN_R));

        double dx = x - crX, dz = z - crZ;
        double dist = Math.sqrt(dx*dx + dz*dz);
        if (dist >= r) continue;

        double t = dist / r;
        double maxD = CRATER_MAX_DEPTH * (r / (double) CRATER_MAX_R);
        double u = 1.0 - t;
        double smooth = u * u * (3.0 - 2.0 * u);
        double depth = maxD * smooth * smooth;

        double noise = ((hashCell(x, z, seed + 999) & 0xFF) / 255.0 - 0.5) * 0.15;
        depth *= (1.0 + noise);

        if (t > 0.8 && t < 1.0)
        {
          double rimT = (t - 0.8) / 0.2;
          double rimShape = Math.sin(rimT * Math.PI);
          depth = Math.min(depth, -maxD * 0.12 * rimShape);
        }

        maxDep = Math.max(maxDep, depth);
      }

    return (int) Math.round(maxDep);
  }

  private static long hashCell(long cx, long cz, long seed)
  {
    long h = seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0x6C62272E07BB0142L);
    h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL;
    h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L;
    h ^= h >>> 33;
    return h;
  }

  private int getBiomeType(int x, int z)
  {
    return (biomeSource instanceof AridBiomeSource abs) ? abs.getBiomeIndex(x, z) : AridBiomeSource.DUST;
  }

  private float getCanyonValue(int x, int z)
  {
    float fx = x * 0.0055f, fz = z * 0.0055f;
    float dwx = AridNoise.fbm(fx + 3.7f, fz + 1.3f, seed + 30L, 3, 0.5f) * 1.8f;
    float dwz = AridNoise.fbm(fx + 8.1f, fz + 5.2f, seed + 31L, 3, 0.5f) * 1.8f;
    float main = AridNoise.fbm(fx + dwx, fz + dwz, seed + 3L, 4, 0.52f);
    return Math.abs(main - 0.50f) * 2f;
  }

  private void placeStrataColumn(ChunkAccess chunk, int wx, int wz, int surfaceY, BlockPos.MutableBlockPos mpos)
  {
    BlockState[] strata =
    {
      ModBlocks.ARID_STRATA_RED.get()   .defaultBlockState(),
      ModBlocks.ARID_STRATA_ORANGE.get().defaultBlockState(),
      ModBlocks.ARID_STRATA_WHITE.get() .defaultBlockState(),
      ModBlocks.ARID_STRATA_MAROON.get().defaultBlockState()
    };

    int yMin = Math.max(CANYON_FLOOR - 2, MIN_Y + 5);
    int yMax = Math.min(PLATEAU_Y + 2, surfaceY - 1);
    for (int y = yMin; y < yMax; y++)
    {
      float wave = AridNoise.fbm(wx * 0.08f, wz * 0.08f, seed + 7L + y * 3, 2, 0.5f) * 3f;
      int idx = ((y + (int) wave) / 4) & 3;
      mpos.set(wx, y, wz);
      if (!chunk.getBlockState(mpos).isAir()) chunk.setBlockState(mpos, strata[idx], false);
    }
  }
}