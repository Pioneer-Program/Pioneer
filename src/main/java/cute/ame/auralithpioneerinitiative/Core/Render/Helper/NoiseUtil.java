package cute.ame.auralithpioneerinitiative.Core.Render.Helper;

public final class NoiseUtil
{
    public static float pseudoRandom3(int x, int y, int z, long seed)
    {
        long h = seed ^ (long) x * 0x9E3779B97F4A7C15L ^ (long) y * 0x6C62272E07BB0142L ^ (long) z * 0xD1B54A32D192ED03L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (float) (h & 0x7FFF_FFFFL) / (float) 0x7FFF_FFFFL;
    }

    public static float valueNoise3(float x, float y, float z, long seed)
    {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
        float fx = x - ix, fy = y - iy, fz = z - iz;
        float ux = fx * fx * (3f - 2f * fx);
        float uy = fy * fy * (3f - 2f * fy);
        float uz = fz * fz * (3f - 2f * fz);

        float c000 = pseudoRandom3(ix, iy, iz, seed);
        float c100 = pseudoRandom3(ix + 1, iy, iz, seed);
        float c010 = pseudoRandom3(ix, iy + 1, iz, seed);
        float c110 = pseudoRandom3(ix + 1, iy + 1, iz, seed);
        float c001 = pseudoRandom3(ix, iy, iz + 1, seed);
        float c101 = pseudoRandom3(ix + 1, iy, iz + 1, seed);
        float c011 = pseudoRandom3(ix, iy + 1, iz + 1, seed);
        float c111 = pseudoRandom3(ix + 1, iy + 1, iz + 1, seed);

        float x00 = lerp(c000, c100, ux);
        float x10 = lerp(c010, c110, ux);
        float x01 = lerp(c001, c101, ux);
        float x11 = lerp(c011, c111, ux);
        float y0 = lerp(x00, x10, uy);
        float y1 = lerp(x01, x11, uy);
        return lerp(y0, y1, uz);
    }

    public static float fbm3(float x, float y, float z, long seed, int octaves, float roughness)
    {
        float v = 0, a = 0.5f, f = 1f, n = 0;
        for (int i = 0; i < octaves; i++)
        {
            v += valueNoise3(x * f, y * f, z * f, seed + i * 13337L) * a;
            n += a; a *= roughness; f *= 2;
        }
        return v / n;
    }

    public static float smoothStep(float t) { return t * t * (3f - 2f * t); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float clamp01(float v) { return Math.clamp(v, 0f, 1f); }

    public static int toRGBA(float r, float g, float b, float a)
    {
        return ((int) (clamp01(a) * 255f) << 24) | ((int) (clamp01(b) * 255f) << 16) | ((int) (clamp01(g) * 255f) << 8) |  (int) (clamp01(r) * 255f);
    }

    public static void faceDir(int face, int x, int y, int res, float[] out)
    {
        float invRes = 1f / res;
        float u = (x + 0.5f) * 2f * invRes - 1f;
        float v = (y + 0.5f) * 2f * invRes - 1f;
        float dx, dy, dz;
        switch (face)
        {
            case CubemapTextures.FACE_FRONT -> { dx =  u;  dy = -v; dz =  1f; }
            case CubemapTextures.FACE_BACK -> { dx = -u;  dy = -v; dz = -1f; }
            case CubemapTextures.FACE_LEFT -> { dx = -1f; dy = -v; dz =  u;  }
            case CubemapTextures.FACE_RIGHT -> { dx =  1f; dy = -v; dz = -u;  }
            case CubemapTextures.FACE_TOP -> { dx =  u;  dy =  1f; dz =  v; }
            case CubemapTextures.FACE_BOTTOM -> { dx =  u;  dy = -1f; dz = -v; }
            default -> throw new IllegalArgumentException("face=" + face);
        }
        float invLen = 1f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        out[0] = dx * invLen;
        out[1] = dy * invLen;
        out[2] = dz * invLen;
    }
}
