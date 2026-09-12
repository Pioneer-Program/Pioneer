package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;

public final class FluidPhase
{
    private static final double R_JOULES = 8.314462618;
    private static final float MINIMUM_MOLES = 1.0e-6f;
    public static final double DEFAULT_CONTAINMENT_P = 1.0;

    public static boolean update(FluidNodeStore store, int nodeId, SpeciesTable table, double containmentP)
    {
        float moles = store.moles(nodeId);
        if (moles < MINIMUM_MOLES)
        {
            store.setLatent(nodeId, 0.0f);
            store.setVapourPressure(nodeId, 0.0f);
            store.setFlag(nodeId, FluidNodeStore.FLAG_LIQUID, false);
            return false;
        }

        double heatCapacity = FluidHeat.capacity(store, nodeId, table.molarHeatRaw());
        if (heatCapacity <= 0.0) return false;

        boolean liquid = store.isLiquid(nodeId);
        float temperature = store.temperature(nodeId);

        double reference = liquid ? Math.max(containmentP, 1.0e-6) : store.pressure(nodeId);
        double boiling = boilingPoint(store, nodeId, table, reference);
        store.setVapourPressure(nodeId, liquid ? (float) vapourPressure(store, nodeId, table, temperature) : 0.0f);

        double latentTotal = latentHeat(store, nodeId, table);
        if (latentTotal <= 0.0) return false;

        double bank = store.latent(nodeId);
        double excess = (temperature - boiling) * heatCapacity;

        if (liquid)
        {
            if (excess > 0.0)
            {
                bank += excess;
                store.setTemperature(nodeId, (float) boiling);
            }
            else if (bank > 0.0)
            {
                bank += excess;
                store.setTemperature(nodeId, (float) boiling);
                if (bank < 0.0)
                {
                    store.setTemperature(nodeId, (float) (boiling + bank / heatCapacity));
                    bank = 0.0;
                }
            }

            if (bank >= latentTotal)
            {
                store.setFlag(nodeId, FluidNodeStore.FLAG_LIQUID, false);
                store.setVapourPressure(nodeId, 0.0f);
                store.setTemperature(nodeId, (float) (boiling + (bank - latentTotal) / heatCapacity));
                store.setLatent(nodeId, 0.0f);
                return true;
            }
        }
        else
        {
            double deficit = -excess;

            if (deficit > 0.0)
            {
                bank += deficit;
                store.setTemperature(nodeId, (float) boiling);
            }
            else if (bank > 0.0)
            {
                bank += deficit;
                store.setTemperature(nodeId, (float) boiling);
                if (bank < 0.0)
                {
                    store.setTemperature(nodeId, (float) (boiling - bank / heatCapacity));
                    bank = 0.0;
                }
            }

            if (bank >= latentTotal)
            {
                store.setFlag(nodeId, FluidNodeStore.FLAG_LIQUID, true);
                store.setTemperature(nodeId, (float) (boiling - (bank - latentTotal) / heatCapacity));
                store.setVapourPressure(nodeId, (float) vapourPressure(store, nodeId, table, store.temperature(nodeId)));
                store.setLatent(nodeId, 0.0f);
                return true;
            }
        }

        store.setLatent(nodeId, (float) bank);
        return false;
    }

    public static double boilingPoint(FluidNodeStore store, int nodeId, SpeciesTable table, double pressureP)
    {
        int stride = Math.min(store.getStride(), table.size());
        float moles = store.moles(nodeId);
        if (moles <= 0.0f) return 0.0;

        double weighted = 0.0;
        double latentWeighted = 0.0;

        for (int s = 0; s < stride; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol <= 0.0f) continue;

            weighted += mol * table.boilingPoint(s);
            latentWeighted += mol * table.latentHeat(s);
        }

        double base = weighted / moles;
        double latentMolar = latentWeighted / moles;

        if (latentMolar <= 0.0 || base <= 0.0) return base;
        if (pressureP <= 1.0e-6) return 0.0;

        double inverse = 1.0 / base - (R_JOULES / latentMolar) * Math.log(pressureP);
        return inverse <= 0.0 ? Double.MAX_VALUE : 1.0 / inverse;
    }

    public static double latentHeat(FluidNodeStore store, int nodeId, SpeciesTable table)
    {
        int stride = Math.min(store.getStride(), table.size());
        double sum = 0.0;

        for (int s = 0; s < stride; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol > 0.0f) sum += mol * table.latentHeat(s);
        }

        return sum;
    }

    public static double vapourPressure(FluidNodeStore store, int nodeId, SpeciesTable table, double temperature)
    {
        double volume = Math.max(store.volume(nodeId), FluidConstants.MIN_VOLUME_L);
        double everything = store.moles(nodeId) * FluidConstants.R * temperature / volume;

        return Math.min(saturationPressure(store, nodeId, table, temperature), everything);
    }

    public static double selfContainment(FluidNodeStore store, int nodeId, SpeciesTable table)
    {
        return vapourPressure(store, nodeId, table, store.temperature(nodeId));
    }

    public static double saturationPressure(FluidNodeStore store, int nodeId, SpeciesTable table, double temperature)
    {
        int stride = Math.min(store.getStride(), table.size());
        float moles = store.moles(nodeId);
        if (moles <= 0.0f || temperature <= 0.0) return 0.0;

        double weighted = 0.0;
        double latentWeighted = 0.0;

        for (int s = 0; s < stride; s++)
        {
            float mol = store.amount(nodeId, s);
            if (mol <= 0.0f) continue;

            weighted += mol * table.boilingPoint(s);
            latentWeighted += mol * table.latentHeat(s);
        }

        double base = weighted / moles;
        double latentMolar = latentWeighted / moles;
        if (base <= 0.0 || latentMolar <= 0.0) return 0.0;

        double pressure = Math.exp((latentMolar / R_JOULES) * (1.0 / base - 1.0 / temperature));
        return Double.isFinite(pressure) ? Math.min(pressure, 1000.0) : 0.0;
    }
}
