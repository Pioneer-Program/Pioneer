package cute.ame.pioneer.Item.Readout;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Readout.Readout;
import cute.ame.pioneer.Core.Readout.ReadoutSource;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.Network.MultiToolReadoutPayload;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class MultiToolReadouts
{
    private static final Map<UUID, Watch> WATCHES = new HashMap<>();
    private static final Readout BUFFER = new Readout();
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

    public static void watch(ServerPlayer player, boolean watching, long pos)
    {
        if (!watching)
        {
            WATCHES.remove(player.getUUID());
            return;
        }

        Watch watch = WATCHES.computeIfAbsent(player.getUUID(), id -> new Watch());
        watch.pos = pos;
        watch.shown = false;
        refresh(player, watch);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (WATCHES.isEmpty()) return;

        MinecraftServer server = event.getServer();
        if (server.getTickCount() % Math.max(Config.MULTITOOL_READOUT_PERIOD_TICKS.get(), 1) != 0) return;

        Iterator<Map.Entry<UUID, Watch>> entries = WATCHES.entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<UUID, Watch> entry = entries.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (player == null) entries.remove();
            else refresh(player, entry.getValue());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        WATCHES.remove(event.getEntity().getUUID());
    }

    private static void refresh(ServerPlayer player, Watch watch)
    {
        BUFFER.clear();

        BlockPos pos = CURSOR.set(watch.pos);
        ServerLevel level = player.serverLevel();

        if (!MultiToolItem.held(player).isEmpty() && level.isLoaded(pos) && player.canInteractWithBlock(pos, 1.0))
        {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ReadoutSource source) source.readout(level, pos, state, BUFFER);
        }

        long fingerprint = BUFFER.fingerprint();
        if (watch.shown && watch.sent == fingerprint) return;

        watch.sent = fingerprint;
        watch.shown = true;
        PacketDistributor.sendToPlayer(player, MultiToolReadoutPayload.of(watch.pos, BUFFER));
    }

    private static final class Watch
    {
        long pos;
        long sent;
        boolean shown;
    }
}
