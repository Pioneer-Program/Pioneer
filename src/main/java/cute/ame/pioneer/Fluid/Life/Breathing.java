package cute.ame.pioneer.Fluid.Life;

import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.SpeciesTable;

public final class Breathing
{
    public enum Verdict
    {
        OK,
        VACUUM,
        HYPOXIA,
        TOXIC
    }

    public record Result(Verdict verdict, int species, float partialPressure)
    {
        public static final Result OK = new Result(Verdict.OK, -1, 0.0f);

        public boolean breathable() { return verdict == Verdict.OK; }
    }

    public static Result evaluate(FluidNodeStore store, int nodeId, SpeciesTable table, float minPressureP)
    {
        double total = store.pressure(nodeId);
        if (total < minPressureP) return new Result(Verdict.VACUUM, -1, (float) total);

        int stride = Math.min(store.getStride(), table.size());

        for (int s = 0; s < stride; s++)
        {
            float partial = (float) (total * store.fraction(nodeId, s));
            if (partial <= 0.0f) continue;

            float hazard = table.hazardPressure(s);
            if (hazard > 0.0f && partial >= hazard) return new Result(Verdict.TOXIC, s, partial);
        }

        for (int s = 0; s < stride; s++)
        {
            if (!table.breathable(s)) continue;

            float required = table.requiredPressure(s);
            if (required <= 0.0f) continue;

            float partial = (float) (total * store.fraction(nodeId, s));
            if (partial >= required) return Result.OK;
        }

        return new Result(Verdict.HYPOXIA, -1, (float) total);
    }
}
