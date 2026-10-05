package cute.ame.pioneer.Frame.Client;

import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Frame.FrameBodies;
import cute.ame.pioneer.Frame.Network.FrameSyncPayload;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.Arrays;

public final class ClientFrames
{
    private static final FramePose POSE = new FramePose();
    private static FrameGrid grid = new FrameGrid(0, 1, 0);
    private static FrameSyncPayload.Entry[] byCell = new FrameSyncPayload.Entry[grid.cellCount()];
    private static int known;
    private static FrameParent[] parents = new FrameParent[grid.cellCount()];
    @Nullable
    private static SolarSystemDefinition parentSystem;
    @Nullable
    private static FrameSyncPayload.Entry poseEntry;
    private static long poseTick;
    private static double posePartial;

    public static void apply(FrameSyncPayload p)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread())
        {
            minecraft.execute(() -> apply(p));
            return;
        }

        if (p.reset() || !grid.sameAs(p.radius(), p.spacing(), p.y())) setGrid(p.radius(), p.spacing(), p.y());

        poseEntry = null;
        for (int cell : p.removals())
        {
            if (cell < 0 || cell >= byCell.length || byCell[cell] == null) continue;

            byCell[cell] = null;
            known--;
        }

        for (FrameSyncPayload.Entry e : p.upserts())
        {
            if (e.cell() < 0 || e.cell() >= byCell.length) continue;

            if (byCell[e.cell()] == null) known++;
            byCell[e.cell()] = e;
            parents[e.cell()] = null;
        }
    }

    public static void clear()
    {
        setGrid(0, 1, 0);
    }

    private static void setGrid(int radius, int spacing, int y)
    {
        grid = new FrameGrid(radius, Math.max(spacing, 1), y);
        byCell = new FrameSyncPayload.Entry[grid.cellCount()];
        parents = new FrameParent[grid.cellCount()];
        known = 0;
        poseEntry = null;
    }

    public static int known()
    {
        return known;
    }

    public static FrameGrid grid()
    {
        return grid;
    }

    @Nullable
    public static FrameSyncPayload.Entry at(double x, double z)
    {
        if (known == 0) return null;

        int cell = grid.cellAt(x, z);
        return cell < 0 ? null : byCell[cell];
    }

    @Nullable
    public static FramePose pose(@Nullable SolarSystemDefinition system, double x, double z, long tick, double partial)
    {
        FrameSyncPayload.Entry e = at(x, z);
        if (e == null) return null;
        if (e == poseEntry && system == parentSystem && tick == poseTick && partial == posePartial) return POSE;

        POSE.set(e.motion(), parent(system, e), e.fixed(), tick, partial);
        poseEntry = e;
        poseTick = tick;
        posePartial = partial;
        return POSE;
    }

    @Nullable
    private static FrameParent parent(@Nullable SolarSystemDefinition system, FrameSyncPayload.Entry e)
    {
        if (system != parentSystem)
        {
            Arrays.fill(parents, null);
            parentSystem = system;
        }

        if (e.parent().isEmpty() || system == null) return null;

        FrameParent parent = parents[e.cell()];
        if (parent == null) parents[e.cell()] = parent = FrameBodies.find(system, e.parent());

        return parent;
    }

    public static boolean virtualOf(@Nullable SolarSystemDefinition system, double x, double y, double z, long tick, double partial, double[] out)
    {
        FramePose pose = pose(system, x, z, tick, partial);
        if (pose == null) return false;

        FrameSyncPayload.Entry e = poseEntry;
        pose.toSystem(x - grid.centerX(e.cell()), y - grid.centerY(), z - grid.centerZ(e.cell()), out);
        return true;
    }

    @Nullable
    public static Quaternionf skyRotation(@Nullable SolarSystemDefinition system, double x, double z, long tick, double partial, Quaternionf dest)
    {
        FramePose pose = pose(system, x, z, tick, partial);
        if (pose == null || !pose.rotated) return null;

        return dest.set((float) -pose.qx, (float) -pose.qy, (float) -pose.qz, (float) pose.qw);
    }
}
