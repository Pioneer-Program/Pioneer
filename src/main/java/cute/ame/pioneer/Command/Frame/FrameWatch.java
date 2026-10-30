package cute.ame.pioneer.Command.Frame;

import cute.ame.pioneer.Core.API.Frame.FrameAPI;
import cute.ame.pioneer.Core.API.Frame.FrameEvent;
import cute.ame.pioneer.Pioneer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FrameWatch
{
    private static final Set<UUID> WATCHERS = new HashSet<>(2);

    public static boolean toggle(ServerPlayer player)
    {
        UUID id = player.getUUID();
        if (WATCHERS.remove(id)) return false;

        WATCHERS.add(id);
        return true;
    }

    @SubscribeEvent
    public static void onCreated(FrameEvent.Created event)
    {
        if (!WATCHERS.isEmpty()) tell(event, "CREATED");
    }

    @SubscribeEvent
    public static void onReleased(FrameEvent.Released event)
    {
        if (!WATCHERS.isEmpty()) tell(event, "RELEASED");
    }

    @SubscribeEvent
    public static void onJoined(FrameEvent.Joined event)
    {
        if (!WATCHERS.isEmpty()) tell(event, "JOINED " + FrameAPI.name(event.ship()));
    }

    @SubscribeEvent
    public static void onSplit(FrameEvent.Split event)
    {
        if (!WATCHERS.isEmpty())
            tell(event, "SPLIT into #" + event.into().id + " ships=" + event.ships().size() + " players=" + event.players() + " (" + event.reason().name().toLowerCase(Locale.ROOT) + ")");
    }

    @SubscribeEvent
    public static void onChanged(FrameEvent.Changed event)
    {
        if (!WATCHERS.isEmpty()) tell(event, event.cause().name());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        WATCHERS.remove(event.getEntity().getUUID());
    }

    private static void tell(FrameEvent event, String what)
    {
        Component line = Component.literal(PREFIX + ChatFormatting.LIGHT_PURPLE + "[watch] " + ChatFormatting.WHITE + "#" + event.frame().id + " " + ChatFormatting.AQUA + what);
        List<ServerPlayer> players = event.level().players();
        for (int i = 0, n = players.size(); i < n; ++i)
        {
            ServerPlayer player = players.get(i);
            if (WATCHERS.contains(player.getUUID())) player.sendSystemMessage(line);
        }
    }
}
