package cute.ame.pioneer.SkyPlanet.Physics;

public final class Blackbody
{
    public static float[] linearRgb(double temperatureK)
    {
        double t = Math.clamp(temperatureK, 1667.0, 25000.0);

        double t2 = t * t, t3 = t2 * t;
        double x = (t <= 4000.0) ? -0.2661239e9 / t3 - 0.2343589e6 / t2 + 0.8776956e3 / t + 0.179910 : -3.0258469e9 / t3 + 2.1070379e6 / t2 + 0.2226347e3 / t + 0.240390;

        double x2 = x * x, x3 = x2 * x;
        double y;
        if (t <= 2222.0) y = -1.1063814 * x3 - 1.34811020 * x2 + 2.18555832 * x - 0.20219683;
        else if (t <= 4000.0) y = -0.9549476 * x3 - 1.37418593 * x2 + 2.09137015 * x - 0.16748867;
        else y =  3.0817580 * x3 - 5.87338670 * x2 + 3.75112997 * x - 0.37001483;

        if (y < 1e-6) return new float[] { 1.0f, 1.0f, 1.0f };

        double bigX = x / y;
        double bigY = 1.0;
        double bigZ = (1.0 - x - y) / y;

        double r =  3.2404542 * bigX - 1.5371385 * bigY - 0.4985314 * bigZ;
        double g = -0.9692660 * bigX + 1.8760108 * bigY + 0.0415560 * bigZ;
        double b =  0.0556434 * bigX - 0.2040259 * bigY + 1.0572252 * bigZ;

        r = Math.max(r, 0.0);
        g = Math.max(g, 0.0);
        b = Math.max(b, 0.0);

        double max = Math.max(r, Math.max(g, b));
        if (max < 1e-9) return new float[] { 1.0f, 1.0f, 1.0f };

        return new float[] { (float) (r / max), (float) (g / max), (float) (b / max) };
    }
}
