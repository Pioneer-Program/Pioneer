package cute.ame.pioneer.Frame.Debug;

import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Core.Frame.FrameQuat;
import cute.ame.pioneer.Frame.Client.ClientFrames;
import cute.ame.pioneer.Frame.FrameBodies;
import cute.ame.pioneer.Frame.Network.FrameSyncPayload;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class FrameClientOverlay
{
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientFrames.clear();
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event)
    {
        if (ClientFrames.known() == 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        List<String> left = event.getLeft();
        left.add("");
        left.add("Pioneer frame");
        FrameSyncPayload.Entry e = ClientFrames.at(player.getX(), player.getZ());
        if (e == null)
        {
            left.add(String.format(Locale.ROOT, "none (%d known)", ClientFrames.known()));
            return;
        }

        float partial = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        long tick = minecraft.level.getGameTime();
        FrameGrid grid = ClientFrames.grid();
        FrameMotion m = e.motion();

        SolarSystemDefinition system = FrameBodies.systemOf(minecraft.level.dimension());
        double[] feet = new double[3], cam = new double[3];
        ClientFrames.virtualOf(system, player.getX(), player.getY(), player.getZ(), tick, partial, feet);
        Vec3 c = minecraft.gameRenderer.getMainCamera().getPosition();
        ClientFrames.virtualOf(system, c.x, c.y, c.z, tick, partial, cam);

        FramePose pose = ClientFrames.pose(system, c.x, c.z, tick, partial);
        double[] q = pose == null ? FrameQuat.identity(new double[4]) : pose.orientation(new double[4]);

        left.add(String.format(Locale.ROOT, "id #%d cell %d,%d (%d known)", e.id(), grid.ix(e.cell()), grid.iz(e.cell()), ClientFrames.known()));
        left.add(e.parent().isEmpty() ? "parent none" : "parent " + e.parent() + (e.fixed() ? " (fixed)" : ""));
        left.add(String.format(Locale.ROOT, "vfeet %.2f %.2f %.2f", feet[0], feet[1], feet[2]));
        left.add(String.format(Locale.ROOT, "vcam %.2f %.2f %.2f", cam[0], cam[1], cam[2]));
        left.add(String.format(Locale.ROOT, "speed %.1f u/s", m.speedAt(tick) * 20.0));
        left.add(String.format(Locale.ROOT, "accel %.1f u/s2", m.accel() * 400.0));
        left.add(String.format(Locale.ROOT, "spin %.1f deg/s", Math.toDegrees(m.spinRate()) * 20.0));
        left.add(String.format(Locale.ROOT, "tilt %.1f deg", FrameQuat.angleDegrees(q[0], q[1], q[2], q[3])));
        left.add(FrameBodies.nearestText(system, cam[0], cam[1], cam[2], tick, partial));
    }
}
