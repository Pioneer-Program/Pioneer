package cute.ame.pioneer.Core.Render.Helper;

public final class NoiseUtil
{
    private static final float R00 = 0.00f, R01 = 0.80f, R02 = 0.60f;
    private static final float R10 = -0.80f, R11 = 0.36f, R12 = -0.48f;
    private static final float R20 = -0.60f, R21 = -0.48f, R22 = 0.64f;

    public static long hash3(int x, int y, int z, long seed)
    {
        long h = seed + x * 0x9E3779B97F4A7C15L + y * 0x6C62272E07BB0142L + z * 0xD1B54A32D192ED03L;
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    public static float pseudoRandom3(int x, int y, int z, long seed)
    {
        return (hash3(x, y, z, seed) >>> 40) * 0x1p-24f;
    }

    private static int fastFloor(float v)
    {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    public static float valueNoise3(float x, float y, float z, long seed)
    {
        int ix = fastFloor(x), iy = fastFloor(y), iz = fastFloor(z);
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
        return lerp(lerp(x00, x10, uy), lerp(x01, x11, uy), uz);
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

    private static float fade(float t)
    {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static float grad(long h, float x, float y, float z)
    {
        int i = (int) (h >>> 59) & 15;
        float u = i < 8 ? x : y;
        float v = i < 4 ? y : (i == 12 || i == 14) ? x : z;
        return ((i & 1) == 0 ? u : -u) + ((i & 2) == 0 ? v : -v);
    }

    public static float perlin3(float x, float y, float z, long seed)
    {
        int ix = fastFloor(x), iy = fastFloor(y), iz = fastFloor(z);
        float fx = x - ix, fy = y - iy, fz = z - iz;
        float gx = fx - 1f, gy = fy - 1f, gz = fz - 1f;
        float ux = fade(fx), uy = fade(fy), uz = fade(fz);

        float n000 = grad(hash3(ix, iy, iz, seed), fx, fy, fz);
        float n100 = grad(hash3(ix + 1, iy, iz, seed), gx, fy, fz);
        float n010 = grad(hash3(ix, iy + 1, iz, seed), fx, gy, fz);
        float n110 = grad(hash3(ix + 1, iy + 1, iz, seed), gx, gy, fz);
        float n001 = grad(hash3(ix, iy, iz + 1, seed), fx, fy, gz);
        float n101 = grad(hash3(ix + 1, iy, iz + 1, seed), gx, fy, gz);
        float n011 = grad(hash3(ix, iy + 1, iz + 1, seed), fx, gy, gz);
        float n111 = grad(hash3(ix + 1, iy + 1, iz + 1, seed), gx, gy, gz);

        float x00 = lerp(n000, n100, ux);
        float x10 = lerp(n010, n110, ux);
        float x01 = lerp(n001, n101, ux);
        float x11 = lerp(n011, n111, ux);
        return lerp(lerp(x00, x10, uy), lerp(x01, x11, uy), uz) * 1.1547f;
    }


    public static float fbmP(float x, float y, float z, long seed, int octaves, float roughness)
    {
        float v = 0f, a = 0.5f, n = 0f;
        for (int i = 0; i < octaves; i++)
        {
            v += perlin3(x, y, z, seed + i * 13337L) * a;
            n += a; a *= roughness;

            float nx = (R00 * x + R01 * y + R02 * z) * 2f;
            float ny = (R10 * x + R11 * y + R12 * z) * 2f;
            float nz = (R20 * x + R21 * y + R22 * z) * 2f;
            x = nx; y = ny; z = nz;
        }
        return v / n;
    }

    public static float fbmP01(float x, float y, float z, long seed, int octaves, float roughness)
    {
        return fbmP(x, y, z, seed, octaves, roughness) * 0.5f + 0.5f;
    }

    public static float ridged3(float x, float y, float z, long seed, int octaves, float roughness)
    {
        float v = 0f, a = 0.5f, n = 0f, prev = 1f;
        for (int i = 0; i < octaves; i++)
        {
            float r = 1f - Math.abs(perlin3(x, y, z, seed + i * 13337L));
            r *= r;
            v += r * a * prev;
            prev = r;
            n += a; a *= roughness;

            float nx = (R00 * x + R01 * y + R02 * z) * 2f;
            float ny = (R10 * x + R11 * y + R12 * z) * 2f;
            float nz = (R20 * x + R21 * y + R22 * z) * 2f;
            x = nx; y = ny; z = nz;
        }
        return clamp01(v / n);
    }

    public static float smoothStep(float t)
    {
        return t * t * (3f - 2f * t);
    }

    public static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    public static float clamp01(float v)
    {
        return Math.clamp(v, 0f, 1f);
    }

    public static int toRGBA(float r, float g, float b, float a)
    {
        return ((int) (clamp01(a) * 255f) << 24) | ((int) (clamp01(b) * 255f) << 16)
                | ((int) (clamp01(g) * 255f) << 8) | (int) (clamp01(r) * 255f);
    }

    public static void faceDir(int face, int x, int y, int res, float[] out)
    {
        float scale = 2f / Math.max(res - 1, 1);
        faceDirUV(face, x * scale - 1f, y * scale - 1f, out);
    }

    public static void faceDirUV(int face, float u, float v, float[] out)
    {
        float dx, dy, dz;
        switch (face)
        {
            case CubemapTextures.FACE_FRONT -> { dx = -u; dy = -v; dz = 1f; }
            case CubemapTextures.FACE_BACK -> { dx = u; dy = -v; dz = -1f; }
            case CubemapTextures.FACE_LEFT -> { dx = -1f; dy = -v; dz = -u; }
            case CubemapTextures.FACE_RIGHT -> { dx = 1f; dy = -v; dz = u; }
            case CubemapTextures.FACE_TOP -> { dx = u; dy = 1f; dz = -v; }
            case CubemapTextures.FACE_BOTTOM -> { dx = u; dy = -1f; dz = v; }
            default -> throw new IllegalArgumentException("face=" + face);
        }
        float invLen = 1f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        out[0] = dx * invLen;
        out[1] = dy * invLen;
        out[2] = dz * invLen;
    }
}