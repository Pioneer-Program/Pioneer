package cute.ame.pioneer.Fluid.Event;

import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Helper.AmbientResolver;
import cute.ame.pioneer.Fluid.Physics.AmbientEqualizer;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
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
        int open = store.getOpenCount();
        if (open == 0) return;

        AmbientState ambient = AmbientResolver.of(level.dimension());
        float rate = Config.AMBIENT_EQUALIZE_RATE.get().floatValue();
        float epsilon = Config.AMBIENT_EQUALIZE_EPSILON.get().floatValue();

        int[] openNodes = store.getOpenNodesRaw();

        for (int i = 0; i < open; i++)
        {
            int id = openNodes[i];

            if (AmbientEqualizer.equalize(store, id, ambient, rate, epsilon)) data.touch(id);
        }
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
