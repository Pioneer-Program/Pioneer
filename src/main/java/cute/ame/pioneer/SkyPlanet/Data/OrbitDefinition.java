package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record OrbitDefinition(
    double radius,
    double periodDays,
    double startAngle,
    double inclination,
    double eccentricity,
    double ascendingNode
)
{
    public static final Codec<OrbitDefinition> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("radius").forGetter(OrbitDefinition::radius),
            Codec.DOUBLE.fieldOf("period_days").forGetter(OrbitDefinition::periodDays),
            Codec.DOUBLE.optionalFieldOf("start_angle",    0.0).forGetter(OrbitDefinition::startAngle),
            Codec.DOUBLE.optionalFieldOf("inclination",    0.0).forGetter(OrbitDefinition::inclination),
            Codec.DOUBLE.optionalFieldOf("eccentricity",   0.0).forGetter(OrbitDefinition::eccentricity),
            Codec.DOUBLE.optionalFieldOf("ascending_node", 0.0).forGetter(OrbitDefinition::ascendingNode)
        ).apply(instance, OrbitDefinition::new)
    );

    private double trueAnomaly(long absoluteTick, float partialTick)
    {
        double ticksPerOrbit = periodDays * 24_000.0;
        double M = ((absoluteTick + partialTick) % ticksPerOrbit) / ticksPerOrbit * 2.0 * Math.PI + startAngle;

        if (eccentricity < 1e-6) return M;

        double E = M;
        for (int iter = 0; iter < 8; iter++)  E -= (E - eccentricity * Math.sin(E) - M) / (1.0 - eccentricity * Math.cos(E));
        double sinV = Math.sqrt(1.0 - eccentricity * eccentricity) * Math.sin(E) / (1.0 - eccentricity * Math.cos(E));
        double cosV = (Math.cos(E) - eccentricity) / (1.0 - eccentricity * Math.cos(E));
        return Math.atan2(sinV, cosV);
    }

    public double computeAngle(long absoluteTick, float partialTick)
    {
        return trueAnomaly(absoluteTick, partialTick);
    }

    public double computeCurrentRadius(double trueAnomaly)
    {
        if (eccentricity < 1e-6) return radius;
        return radius * (1.0 - eccentricity * eccentricity) / (1.0 + eccentricity * Math.cos(trueAnomaly - startAngle));
    }

    public float[] compute3DPosition(double trueAnomaly, double r, float skyScale)
    {
        double ox = r * Math.cos(trueAnomaly);
        double oz = r * Math.sin(trueAnomaly);

        double i = inclination;
        double Omega = ascendingNode;

        double wx = ox;
        double wy = oz * Math.sin(i);
        double wz = oz * Math.cos(i);

        double cosO = Math.cos(Omega), sinO = Math.sin(Omega);
        float x = (float) ((wx * cosO - wz * sinO) * skyScale);
        float y = (float) (wy * skyScale);
        float z = (float) ((wx * sinO + wz * cosO) * skyScale);

        return new float[]{x, y, z};
    }
}