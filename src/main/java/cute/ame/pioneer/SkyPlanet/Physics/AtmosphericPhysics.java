package cute.ame.pioneer.SkyPlanet.Physics;

import cute.ame.pioneer.SkyPlanet.Data.GasDefinition;
import cute.ame.pioneer.SkyPlanet.Registry.GasRegistry;

import java.util.Map;
import java.util.function.ToDoubleFunction;

public final class AtmosphericPhysics
{
    private static final double R_GAS = 8.314462618;
    private static final double LAMBDA_R = 680.0, LAMBDA_G = 550.0, LAMBDA_B = 440.0;

    public static GasDefinition gas(String name)
    {
        return GasRegistry.get(name);
    }

    public static boolean isKnownGas(String name)
    {
        return GasRegistry.isKnown(name);
    }

    private static double weightedMean(Map<String, Float> composition, ToDoubleFunction<GasDefinition> property, double fallback)
    {
        double total = 0.0, sum = 0.0;
        for (Map.Entry<String, Float> e : composition.entrySet())
        {
            double f = Math.max(e.getValue(), 0.0);
            total += f;
            sum += f * property.applyAsDouble(gas(e.getKey()));
        }
        return (total > 1e-9) ? sum / total : fallback;
    }

    public static double meanMolarMass(Map<String, Float> composition)
    {
        return weightedMean(composition, GasDefinition::molarMassGPerMol, 28.97);
    }

    public static double meanRefractivity(Map<String, Float> composition)
    {
        return weightedMean(composition, GasDefinition::refractivity1e8, 29000.0);
    }

    public static double ozoneFraction(Map<String, Float> composition)
    {
        double total = 0.0, sum = 0.0;
        for (Map.Entry<String, Float> e : composition.entrySet())
        {
            double f = Math.max(e.getValue(), 0.0);
            total += f;
            if (gas(e.getKey()).producesOzone()) sum += f;
        }
        return (total > 1e-9) ? sum / total : 0.0;
    }

    public static double methaneFraction(Map<String, Float> composition)
    {
        return Math.min(1.0, weightedMean(composition, g -> g.greenhousePotency() / 25.0, 0.0));
    }

    public static double greenhouseFraction(Map<String, Float> composition)
    {
        return Math.min(1.0, weightedMean(composition, GasDefinition::greenhousePotency, 0.0));
    }

    public static double meanSpecificHeat(Map<String, Float> composition)
    {
        double massTotal = 0.0, sum = 0.0;
        for (Map.Entry<String, Float> e : composition.entrySet())
        {
            double f = Math.max(e.getValue(), 0.0);
            GasDefinition g = gas(e.getKey());
            double m = f * g.molarMassGPerMol();
            massTotal += m;
            sum += m * g.specificHeat();
        }
        return (massTotal > 1e-9) ? sum / massTotal : 1005.0;
    }

    private static final double RAYLEIGH_K = 2.85e19;

    public static double[] rayleighOpticalDepth(Map<String, Float> composition, double surfacePressureBar, double gravityMs2)
    {
        double nr = meanRefractivity(composition) * 1e-8;
        double column = Math.max(surfacePressureBar, 0.0) / (Math.max(meanMolarMass(composition), 1e-3) * Math.max(gravityMs2, 1e-3));
        double base = RAYLEIGH_K * nr * nr * column;

        return new double[]
        {
            base / Math.pow(LAMBDA_R, 4.0),
            base / Math.pow(LAMBDA_G, 4.0),
            base / Math.pow(LAMBDA_B, 4.0)
        };
    }

    public static float[] skyColorRgb(Map<String, Float> composition, double surfacePressureBar, double gravityMs2, float[] hazeColor, double hazeOpticalDepth)
    {
        double[] tau = rayleighOpticalDepth(composition, surfacePressureBar, gravityMs2);

        double ch4 = methaneFraction(composition);
        tau[0] *= (1.0 - 0.85 * ch4);
        tau[1] *= (1.0 - 0.25 * ch4);

        double max = Math.max(tau[0], Math.max(tau[1], tau[2]));
        float[] rayleigh = (max < 1e-30) ? new float[] { 0.4f, 0.6f, 1.0f } : new float[] { (float) (tau[0] / max), (float) (tau[1] / max), (float) (tau[2] / max) };
        if (hazeColor == null || hazeOpticalDepth <= 1e-6) return rayleigh;

        double rayleighDepth = Math.max(tau[1], 1e-9);
        double w = hazeOpticalDepth / (hazeOpticalDepth + rayleighDepth);

        return new float[]
        {
            (float) (rayleigh[0] * (1.0 - w) + hazeColor[0] * w),
            (float) (rayleigh[1] * (1.0 - w) + hazeColor[1] * w),
            (float) (rayleigh[2] * (1.0 - w) + hazeColor[2] * w)
        };
    }

    public static double scaleHeightMetres(double meanMolarMassGPerMol, double temperatureK, double gravityMs2)
    {
        double m = Math.max(meanMolarMassGPerMol, 1e-3) * 1e-3;
        double g = Math.max(gravityMs2, 1e-3);
        return R_GAS * Math.max(temperatureK, 1.0) / (m * g);
    }

    public static float opacityFromDepth(double rayleighDepthGreen, double hazeOpticalDepth)
    {
        double total = Math.max(rayleighDepthGreen, 0.0) + Math.max(hazeOpticalDepth, 0.0);
        return (float) Math.clamp(1.0 - Math.exp(-12.0 * total), 0.0, 1.0);
    }
}
