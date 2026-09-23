package cute.ame.pioneer.Thermal.Data;

public final class MaterialTable
{
    public static final int DEFAULT = 0;
    public static final MaterialTable EMPTY = new MaterialTable(
        new String[] { "pioneer:default" },
        new float[] { ThermalMaterial.FALLBACK.conductivity() },
        new float[] { ThermalMaterial.FALLBACK.volumetricHeat() },
        new float[] { ThermalMaterial.NEVER_BREAKS },
        new float[] { ThermalMaterial.FALLBACK.emissivity() },
        new float[] { ThermalMaterial.FALLBACK.areaFactor() }
    );

    private final String[] keys;
    private final float[] conductivity;
    private final float[] volumetricHeat;
    private final float[] breakdownK;
    private final float[] emissivity;
    private final float[] areaFactor;

    public MaterialTable(String[] keys, float[] conductivity, float[] volumetricHeat, float[] breakdownK, float[] emissivity, float[] areaFactor)
    {
        this.keys = keys;
        this.conductivity = conductivity;
        this.volumetricHeat = volumetricHeat;
        this.breakdownK = breakdownK;
        this.emissivity = emissivity;
        this.areaFactor = areaFactor;
    }

    public int size()
    {
        return keys.length;
    }

    public boolean isValid(int material)
    {
        return material >= 0 && material < keys.length;
    }

    public String key(int material)
    {
        return keys[material];
    }

    public float conductivity(int material)
    {
        return conductivity[material];
    }

    public float volumetricHeat(int material)
    {
        return volumetricHeat[material];
    }

    public float breakdownK(int material)
    {
        return breakdownK[material];
    }

    public float emissivity(int material)
    {
        return emissivity[material];
    }

    public float areaFactor(int material)
    {
        return areaFactor[material];
    }

    public String[] keysRaw()
    {
        return keys;
    }

    public float[] conductivityRaw()
    {
        return conductivity;
    }

    public float[] volumetricHeatRaw()
    {
        return volumetricHeat;
    }

    public float[] breakdownRaw()
    {
        return breakdownK;
    }

    public float[] emissivityRaw()
    {
        return emissivity;
    }

    public float[] areaFactorRaw()
    {
        return areaFactor;
    }

    public static float couple(float a, float b)
    {
        float sum = a + b;
        return sum <= 0.0f ? 0.0f : 2.0f * a * b / sum;
    }
}
