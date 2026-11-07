package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Frame.Network.FrameSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class FrameSync
{
    private static final int[] NONE = new int[0];

    public static void reset(ServerPlayer player)
    {
        reset(player, player.serverLevel());
    }

    public static void reset(ServerPlayer player, ServerLevel level)
    {
        FrameManager manager = PioneerAPI.isSpaceDimension(level.dimension()) ? FrameManager.get(level) : null;
        PacketDistributor.sendToPlayer(player, manager == null ? new FrameSyncPayload(true, 0, 1, 0, List.of(), NONE) : full(manager));
    }

    public static void upsert(ServerLevel level, FrameManager manager, LocalFrame frame)
    {
        FrameGrid grid = manager.grid();
        PacketDistributor.sendToPlayersInDimension(level, new FrameSyncPayload(false, grid.radius(), grid.spacing(), grid.y(), List.of(entry(frame)), NONE));
    }

    public static void remove(ServerLevel level, FrameManager manager, int cell)
    {
        FrameGrid grid = manager.grid();
        PacketDistributor.sendToPlayersInDimension(level, new FrameSyncPayload(false, grid.radius(), grid.spacing(), grid.y(), List.of(), new int[]{cell}));
    }

    public static FrameSyncPayload full(FrameManager manager)
    {
        FrameGrid grid = manager.grid();
        List<FrameSyncPayload.Entry> entries = new ArrayList<>(manager.frames().size());
        for (LocalFrame frame : manager.frames()) entries.add(entry(frame));

        return new FrameSyncPayload(true, grid.radius(), grid.spacing(), grid.y(), entries, NONE);
    }

    private static FrameSyncPayload.Entry entry(LocalFrame frame)
    {
        return new FrameSyncPayload.Entry(frame.id, frame.cell(), new FrameMotion().set(frame.motion()), frame.parentName(), frame.fixed());
    }
}
