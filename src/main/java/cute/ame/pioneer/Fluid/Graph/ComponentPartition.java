package cute.ame.pioneer.Fluid.Graph;

import java.util.Arrays;

public final class ComponentPartition
{
    public record Result(int count, int[] nodeOrder, int[] nodeOffsets, int[] edgeOrder, int[] edgeOffsets, int[] componentOf)
    {
        public static final Result EMPTY = new Result(0, new int[0], new int[] { 0 }, new int[0], new int[] { 0 }, new int[0]);

        public int nodeCount(int component)
        { 
            return nodeOffsets[component + 1] - nodeOffsets[component]; 
        }

        public int edgeCount(int component) 
        { 
            return edgeOffsets[component + 1] - edgeOffsets[component]; 
        }

        public int componentOf(int nodeId)
        {
            return (nodeId >= 0 && nodeId < componentOf.length) ? componentOf[nodeId] : -1;
        }
    }

    public static Result of(int[] nodes, int nodeCount, int[] edgeA, int[] edgeB, int edgeCount, int maxNodeId)
    {
        if (nodeCount == 0) return Result.EMPTY;

        int[] degree = new int[maxNodeId + 1];
        for (int e = 0; e < edgeCount; e++)
        {
            degree[edgeA[e]]++;
            degree[edgeB[e]]++;
        }

        int[] adjacencyStart = new int[maxNodeId + 2];
        for (int i = 0; i <= maxNodeId; i++) adjacencyStart[i + 1] = adjacencyStart[i] + degree[i];

        int[] cursor = Arrays.copyOf(adjacencyStart, maxNodeId + 1);
        int[] adjacency = new int[edgeCount * 2];
        for (int e = 0; e < edgeCount; e++)
        {
            adjacency[cursor[edgeA[e]]++] = edgeB[e];
            adjacency[cursor[edgeB[e]]++] = edgeA[e];
        }

        int[] componentOf = new int[maxNodeId + 1];
        Arrays.fill(componentOf, -1);

        int[] nodeOrder = new int[nodeCount];
        int[] nodeOffsets = new int[nodeCount + 1];

        int count = 0;
        int written = 0;

        for (int i = 0; i < nodeCount; i++)
        {
            int seed = nodes[i];
            if (componentOf[seed] != -1) continue;

            nodeOffsets[count] = written;

            componentOf[seed] = count;
            nodeOrder[written++] = seed;

            for (int head = nodeOffsets[count]; head < written; head++)
            {
                int current = nodeOrder[head];

                for (int a = adjacencyStart[current]; a < adjacencyStart[current + 1]; a++)
                {
                    int next = adjacency[a];
                    if (componentOf[next] != -1) continue;

                    componentOf[next] = count;
                    nodeOrder[written++] = next;
                }
            }

            count++;
        }

        nodeOffsets[count] = written;
        nodeOffsets = Arrays.copyOf(nodeOffsets, count + 1);

        int[] edgesPerComponent = new int[count + 1];
        for (int e = 0; e < edgeCount; e++) edgesPerComponent[componentOf[edgeA[e]]]++;

        int[] edgeOffsets = new int[count + 1];
        for (int i = 0; i < count; i++) edgeOffsets[i + 1] = edgeOffsets[i] + edgesPerComponent[i];

        int[] edgeCursor = Arrays.copyOf(edgeOffsets, count);
        int[] edgeOrder = new int[edgeCount];
        for (int e = 0; e < edgeCount; e++) edgeOrder[edgeCursor[componentOf[edgeA[e]]]++] = e;

        return new Result(count, Arrays.copyOf(nodeOrder, written), nodeOffsets, edgeOrder, edgeOffsets, componentOf);
    }
}
