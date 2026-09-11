package cute.ame.pioneer.Fluid.Physics;

import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;

public final class ContainmentCache
{
    private double[] lowest = new double[0];
    private double[] second = new double[0];
    private int[] argLowest = new int[0];
    private int count;

    public void rebuild(FluidNodeStore store, FluidGraph graph, ComponentPartition.Result partition)
    {
        int components = partition.count();

        if (components > lowest.length)
        {
            lowest = new double[components];
            second = new double[components];
            argLowest = new int[components];
        }

        count = components;
        if (components == 0) return;

        int[] nodeOrder = partition.nodeOrder();
        int[] nodeOffsets = partition.nodeOffsets();

        for (int c = 0; c < components; c++)
        {
            if (graph.isAsleep(c))
            {
                lowest[c] = Double.NaN;
                second[c] = Double.NaN;
                argLowest[c] = FluidNodeStore.INVALID;
                continue;
            }

            double best = Double.NaN;
            double next = Double.NaN;
            int owner = FluidNodeStore.INVALID;

            for (int i = nodeOffsets[c], to = nodeOffsets[c + 1]; i < to; i++)
            {
                int id = nodeOrder[i];
                if (!store.alive(id)) continue;

                double pressure = store.pressure(id);

                if (Double.isNaN(best) || pressure < best)
                {
                    next = best;
                    best = pressure;
                    owner = id;
                }
                else if (Double.isNaN(next) || pressure < next)
                {
                    next = pressure;
                }
            }

            lowest[c] = best;
            second[c] = next;
            argLowest[c] = owner;
        }
    }

    public double lowestExcluding(int component, int nodeId)
    {
        if (component < 0 || component >= count) return Double.NaN;

        return nodeId == argLowest[component] ? second[component] : lowest[component];
    }

    public int componentCount()
    {
        return count;
    }
}
