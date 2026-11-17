package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FrameEvents
{
    private static boolean passOffWarned;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        FrameSync.reset(player);
        seat(player, -1);
        adopt(player.serverLevel());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player) seat(player, 1);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        FrameSync.reset(player);
        sweep(player.server.getLevel(event.getFrom()));
        adopt(player.serverLevel());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        FrameSync.reset(player);
        for (ServerLevel level : player.server.getAllLevels()) sweep(level);

        adopt(player.serverLevel());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        int period = Config.FRAME_MAINTAIN_PERIOD.get();
        if (period <= 0)
        {
            warnPassOff(level);
            return;
        }

        passOffWarned = false;
        if (level.getGameTime() % period != 0L) return;

        FrameManager manager = FrameManager.of(level);
        if (manager == null && !level.players().isEmpty() && PioneerAPI.isSpaceDimension(level.dimension()))
            manager = FrameManager.getOrCreate(level);
        if (manager != null) manager.maintain(level);
    }

    private static void warnPassOff(ServerLevel level)
    {
        if (passOffWarned || level.getGameTime() % 20L != 0L) return;

        FrameManager manager = FrameManager.of(level);
        if (manager == null || manager.frames().isEmpty()) return;

        passOffWarned = true;
        Pioneer.LOGGER.warn("frame_maintain_period is 0: no pass runs, ships and players in {} may leave their frames", level.dimension().location());
    }

    private static void adopt(ServerLevel level)
    {
        if (!PioneerAPI.isSpaceDimension(level.dimension())) return;

        FrameManager.getOrCreate(level).adopt(level);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event)
    {
        if (!event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) return;

        Entity entity = event.getEntity();
        if (entity instanceof Player || entity.isPassenger()) return;

        FrameManager manager = FrameManager.of(level);
        if (manager != null) manager.restore(entity);
    }

    public static void stamp(Entity entity)
    {
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof Player || entity.isPassenger()) return;

        FrameManager manager = FrameManager.of(level);
        if (manager != null) manager.stamp(entity);
    }

    private static void seat(ServerPlayer player, int delta)
    {
        ServerLevel level = player.serverLevel();
        FrameManager manager = FrameManager.of(level);
        LocalFrame frame = manager == null ? null : manager.at(player.getX(), player.getZ());
        if (frame != null && frame.auto() && (delta > 0 || frame.absent() > 0)) manager.absent(frame, delta);
    }

    private static void sweep(@Nullable ServerLevel level)
    {
        FrameManager manager = level == null ? null : FrameManager.of(level);
        if (manager != null) manager.sweep(level);
    }
}
