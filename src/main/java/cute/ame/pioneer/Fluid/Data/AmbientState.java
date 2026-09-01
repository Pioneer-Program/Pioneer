package cute.ame.pioneer.Fluid.Data;

public record AmbientState(double pressureP, float temperatureK, float[] fractions, boolean vacuum)
{
    public static final double VACUUM_THRESHOLD_P = 1.0e-4;

    public static AmbientState vacuum(int speciesCount)
    {
        return new AmbientState(0.0, 2.7f, new float[speciesCount], true);
    }

    public float fraction(int species)
    {
        return (species >= 0 && species < fractions.length) ? fractions[species] : 0.0f;
    }
}
