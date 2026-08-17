package cute.ame.pioneer.Core.Render.Helper;

import net.minecraft.resources.ResourceLocation;

public final class CloudShadowParams
{
    private static boolean valid;
    private static ResourceLocation weatherLut;
    private static float coverageBias;
    private static float driftSin;
    private static float driftCos;
    private static float cloudInner;
    private static float strength;

    public static void publish(ResourceLocation lut, float coverageBiasIn, float driftSinIn, float driftCosIn, float cloudInnerIn, float strengthIn)
    {
        weatherLut = lut;
        coverageBias = coverageBiasIn;
        driftSin = driftSinIn;
        driftCos = driftCosIn;
        cloudInner = cloudInnerIn;
        strength = strengthIn;
        valid = lut != null;
    }

    public static void clear()
    {
        valid = false;
        weatherLut = null;
        strength = 0.0f;
    }

    public static boolean valid() { return valid; }
    public static ResourceLocation weatherLut() { return weatherLut; }
    public static float coverageBias() { return coverageBias; }
    public static float driftSin() { return driftSin; }
    public static float driftCos() { return driftCos; }
    public static float cloudInner() { return cloudInner; }
    public static float strength() { return valid ? strength : 0.0f; }
}
