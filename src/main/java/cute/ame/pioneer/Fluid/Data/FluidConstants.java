package cute.ame.pioneer.Fluid.Data;

public final class FluidConstants
{
    public static final double R = 0.0820573;
    public static final float MIN_VOLUME_L = 1.0e-3f;
    public static final float TANK_VOLUME_L = 1000.0f;
    public static final float PIPE_VOLUME_L = 100.0f;
    public static final float DEFAULT_TEMPERATURE_K = 293.15f;
    public static final float ZERO_CELSIUS_K = 273.15f;

    public static float toCelsius(float kelvin)
    {
        return kelvin - ZERO_CELSIUS_K;
    }

    public static float toKelvin(float celsius)
    {
        return celsius + ZERO_CELSIUS_K;
    }
}
