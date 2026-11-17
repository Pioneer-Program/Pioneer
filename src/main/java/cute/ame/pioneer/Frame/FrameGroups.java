package cute.ame.pioneer.Frame;

import java.util.Arrays;

final class FrameGroups
{
    int count, ships, groups;
    int[] group = new int[8];
    double[] box = new double[48], mass = new double[8], pos = new double[24], vel = new double[24];
    double[] groupBox = new double[48], groupMass = new double[8], groupPos = new double[24], groupVel = new double[24];
    int[] groupShips = new int[8], groupPlayers = new int[8], groupFirst = new int[8];
    private int[] parent = new int[8];
    private double[] sums = new double[64];

    private static double gap(double[] a, int i, double[] b, int j)
    {
        int x = i * 6, y = j * 6;
        double dx = axisGap(a[x], a[x + 3], b[y], b[y + 3]);
        double dy = axisGap(a[x + 1], a[x + 4], b[y + 1], b[y + 4]);
        double dz = axisGap(a[x + 2], a[x + 5], b[y + 2], b[y + 5]);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axisGap(double min0, double max0, double min1, double max1)
    {
        if (max0 < min1) return min1 - max0;
        if (max1 < min0) return min0 - max1;

        return 0.0;
    }

    void clear()
    {
        count = ships = groups = 0;
    }

    void add(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double itemMass, double x, double y, double z, double vx, double vy, double vz, boolean ship)
    {
        grow(count + 1);
        int b = count * 6, p = count * 3;
        box[b] = minX;
        box[b + 1] = minY;
        box[b + 2] = minZ;
        box[b + 3] = maxX;
        box[b + 4] = maxY;
        box[b + 5] = maxZ;
        mass[count] = itemMass;
        pos[p] = x;
        pos[p + 1] = y;
        pos[p + 2] = z;
        vel[p] = vx;
        vel[p + 1] = vy;
        vel[p + 2] = vz;
        parent[count] = count;
        if (ship) ++ships;

        ++count;
    }

    void link(double reach)
    {
        for (int i = 0; i < count; ++i)
            for (int j = i + 1; j < count; ++j) if (gap(box, i, box, j) <= reach) union(i, j);

        collect();
    }

    double groupGap(int a, int b)
    {
        return gap(groupBox, a, groupBox, b);
    }

    private void collect()
    {
        groups = 0;
        Arrays.fill(group, 0, count, -1);
        for (int i = 0; i < count; ++i)
        {
            int r = find(i);
            if (group[r] < 0) group[r] = groups++;

            group[i] = group[r];
        }

        growGroups(groups);
        Arrays.fill(sums, 0, groups * 8, 0.0);
        Arrays.fill(groupMass, 0, groups, 0.0);
        Arrays.fill(groupShips, 0, groups, 0);
        Arrays.fill(groupPlayers, 0, groups, 0);
        Arrays.fill(groupFirst, 0, groups, -1);
        for (int i = 0; i < count; ++i) include(i, group[i]);

        for (int g = 0; g < groups; ++g) average(g);
    }

    private void include(int i, int g)
    {
        int b = i * 6, gb = g * 6, p = i * 3, s = g * 8;
        if (groupFirst[g] < 0)
        {
            groupFirst[g] = i;
            System.arraycopy(box, b, groupBox, gb, 6);
        }
        else
        {
            for (int k = 0; k < 3; ++k) groupBox[gb + k] = Math.min(groupBox[gb + k], box[b + k]);
            for (int k = 3; k < 6; ++k) groupBox[gb + k] = Math.max(groupBox[gb + k], box[b + k]);
        }

        if (i >= ships)
        {
            ++groupPlayers[g];
            return;
        }

        ++groupShips[g];
        double w = Math.max(mass[i], 0.0);
        groupMass[g] += w;
        for (int k = 0; k < 3; ++k)
        {
            sums[s + k] += w * pos[p + k];
            sums[s + 3 + k] += w * vel[p + k];
        }

        sums[s + 6] += 1.0;
    }

    private void average(int g)
    {
        int s = g * 8, p = g * 3;
        if (groupMass[g] > 0.0)
        {
            for (int k = 0; k < 3; ++k)
            {
                groupPos[p + k] = sums[s + k] / groupMass[g];
                groupVel[p + k] = sums[s + 3 + k] / groupMass[g];
            }

            return;
        }

        int n = 0;
        Arrays.fill(groupPos, p, p + 3, 0.0);
        Arrays.fill(groupVel, p, p + 3, 0.0);
        boolean shipsOnly = groupShips[g] > 0;
        for (int i = 0; i < count; ++i)
        {
            if (group[i] != g || (shipsOnly && i >= ships)) continue;

            for (int k = 0; k < 3; ++k)
            {
                groupPos[p + k] += pos[i * 3 + k];
                groupVel[p + k] += vel[i * 3 + k];
            }

            ++n;
        }

        for (int k = 0; k < 3; ++k)
        {
            groupPos[p + k] /= n;
            groupVel[p + k] /= n;
        }
    }

    private int find(int i)
    {
        while (parent[i] != i)
        {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }

        return i;
    }

    private void union(int a, int b)
    {
        int ra = find(a), rb = find(b);
        if (ra != rb) parent[Math.max(ra, rb)] = Math.min(ra, rb);
    }

    private void grow(int n)
    {
        if (n <= mass.length) return;

        int size = Math.max(n, mass.length * 2);
        group = Arrays.copyOf(group, size);
        parent = Arrays.copyOf(parent, size);
        mass = Arrays.copyOf(mass, size);
        box = Arrays.copyOf(box, size * 6);
        pos = Arrays.copyOf(pos, size * 3);
        vel = Arrays.copyOf(vel, size * 3);
    }

    private void growGroups(int n)
    {
        if (n <= groupMass.length) return;

        int size = Math.max(n, groupMass.length * 2);
        groupMass = Arrays.copyOf(groupMass, size);
        groupShips = Arrays.copyOf(groupShips, size);
        groupPlayers = Arrays.copyOf(groupPlayers, size);
        groupFirst = Arrays.copyOf(groupFirst, size);
        groupBox = Arrays.copyOf(groupBox, size * 6);
        groupPos = Arrays.copyOf(groupPos, size * 3);
        groupVel = Arrays.copyOf(groupVel, size * 3);
        sums = Arrays.copyOf(sums, size * 8);
    }
}
