package cute.ame.auralithpioneerinitiative.Planet.Arid.Worldgen;

public final class AridNoise
{
  public static float fbm(float x, float y, long seed, int octaves, float roughness)
  {
    float v = 0, a = 0.5f, f = 1f, n = 0;
    for (int i = 0; i < octaves; i++)
    {
      v += valueNoise(x * f, y * f, seed + i * 13337L) * a;
      n += a; a *= roughness; f *= 2;
    }
    return v / n;
  }

  public static float valueNoise(float x, float y, long seed)
  {
    int ix = (int)Math.floor(x), iy = (int)Math.floor(y);
    float fx = x - ix, fy = y - iy;
    float ux = fx * fx * (3f - 2f * fx);
    float uy = fy * fy * (3f - 2f * fy);
    return lerp(
        lerp(hash(ix,   iy,   seed), hash(ix+1, iy,   seed), ux),
        lerp(hash(ix,   iy+1, seed), hash(ix+1, iy+1, seed), ux),
        uy
    );
  }

  public static float hash2(int x, int z, long seed)
  {
    return hash(x, z, seed);
  }

  private static float hash(int x, int y, long seed)
  {
    long h = seed ^ ((long)x * 0x9E3779B97F4A7C15L) ^ ((long)y * 0x6C62272E07BB0142L);
    h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL;
    h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L;
    h ^= h >>> 33;
    return (float)(h & 0x7FFF_FFFFL) / (float)0x7FFF_FFFFL;
  }

  public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
  public static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
