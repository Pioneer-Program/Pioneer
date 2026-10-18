package cute.ame.pioneer.Core.Readout;

import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.celsius.Fluid.Helper.SensorReadings;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class FluidReadout
{
    private static final double MIN_FRACTION = 1.0e-3;

    public static void node(ServerLevel level, BlockPos pos, FluidNodeStore store, int node, Readout out)
    {
        out.number("readout.pioneer.pressure", store.pressure(node), ReadoutUnit.PRESSURE);
        out.number("readout.pioneer.temperature", SensorReadings.temperature(level, pos), ReadoutUnit.CELSIUS);
        out.number("readout.pioneer.moles", store.moles(node), ReadoutUnit.MOLE);
        out.translated("readout.pioneer.phase", store.isLiquid(node) ? "readout.pioneer.phase.liquid" : "readout.pioneer.phase.gas");
        species(store, node, out);
    }

    public static void species(FluidNodeStore store, int node, Readout out)
    {
        SpeciesTable table = FluidSpecies.active();
        int stride = Math.min(store.getStride(), table.size());

        int first = -1, second = -1, third = -1;
        double a = 0.0, b = 0.0, c = 0.0;

        for (int s = 0; s < stride; s++)
        {
            double fraction = store.fraction(node, s);
            if (fraction < MIN_FRACTION) continue;

            if (fraction > a)
            {
                third = second; c = b;
                second = first; b = a;
                first = s; a = fraction;
            }
            else if (fraction > b)
            {
                third = second; c = b;
                second = s; b = fraction;
            }
            else if (fraction > c)
            {
                third = s; c = fraction;
            }
        }

        if (first >= 0) out.number("gas.pioneer." + table.key(first), a * 100.0, ReadoutUnit.PERCENT);
        if (second >= 0) out.number("gas.pioneer." + table.key(second), b * 100.0, ReadoutUnit.PERCENT);
        if (third >= 0) out.number("gas.pioneer." + table.key(third), c * 100.0, ReadoutUnit.PERCENT);
    }
}
