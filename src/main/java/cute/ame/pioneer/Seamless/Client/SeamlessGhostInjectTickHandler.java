package cute.ame.pioneer.Seamless.Client;

import cute.ame.pioneer.Pioneer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class SeamlessGhostInjectTickHandler
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        SeamlessGhostInjectQueue.drain();
    }
}
