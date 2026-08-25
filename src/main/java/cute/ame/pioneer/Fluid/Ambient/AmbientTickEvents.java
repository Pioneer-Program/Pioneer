package cute.ame.pioneer.Fluid.Ambient;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.FluidNodeStore;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class AmbientTickEvents
{
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event)
    {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return;

        FluidNodeStore store = data.store();
        int high = store.getHighWater();
        if (high == 0) return;

        AmbientState ambient = AmbientResolver.of(level.dimension());
        float rate = Config.AMBIENT_EQUALIZE_RATE.get().floatValue();

        boolean touched = false;
        for (int id = 0; id < high; id++)
        {
            if (!store.alive(id) || !store.hasFlag(id, FluidNodeStore.FLAG_OPEN)) continue;

            AmbientEqualizer.equalize(store, id, ambient, rate);
            data.touch(id);
            touched = true;
        }

        if (touched) data.setDirty();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        AmbientResolver.invalidate();
    }

    @SubscribeEvent
    public static void onDatapackReload(OnDatapackSyncEvent event)
    {
        if (event.getPlayer() != null) return;
        AmbientResolver.invalidate();
    }
}
