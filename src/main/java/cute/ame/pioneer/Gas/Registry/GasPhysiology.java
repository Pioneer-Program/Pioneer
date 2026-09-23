package cute.ame.pioneer.Gas.Registry;

import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.pioneer.Gas.Data.GasProperties;
import cute.ame.pioneer.Pioneer;

public final class GasPhysiology
{
    private static volatile boolean[] breathable = new boolean[0];
    private static volatile float[] requiredPressure = new float[0];
    private static volatile float[] hazardPressure = new float[0];

    public static void rebuild(SpeciesTable table)
    {
        int n = table.size();
        boolean[] nextBreathable = new boolean[n];
        float[] nextRequired = new float[n];
        float[] nextHazard = new float[n];

        for (int i = 0; i < n; i++)
        {
            GasProperties gas = GasRegistry.get(table.key(i));
            nextBreathable[i] = gas.breathable();
            nextRequired[i] = gas.requiredFraction();
            nextHazard[i] = gas.hazardFraction();
        }

        breathable = nextBreathable;
        requiredPressure = nextRequired;
        hazardPressure = nextHazard;

        for (String key : GasRegistry.loadedKeys())
        {
            if (table.indexOf(key) == SpeciesTable.UNKNOWN) Pioneer.LOGGER.warn("[Pioneer] gases/{}.json has no matching species/{}.json, its properties are ignored", key, key);
        }
    }

    public static int size()
    {
        return breathable.length;
    }

    public static boolean breathable(int species)
    {
        return species >= 0 && species < breathable.length && breathable[species];
    }

    public static float requiredPressure(int species)
    {
        return (species >= 0 && species < requiredPressure.length) ? requiredPressure[species] : 0.0f;
    }

    public static float hazardPressure(int species)
    {
        return (species >= 0 && species < hazardPressure.length) ? hazardPressure[species] : Float.MAX_VALUE;
    }
}
