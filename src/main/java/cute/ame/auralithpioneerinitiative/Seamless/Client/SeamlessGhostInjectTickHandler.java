package cute.ame.auralithpioneerinitiative.Seamless.Client;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Auralithpioneerinitiative.MODID, value = Dist.CLIENT)
public final class SeamlessGhostInjectTickHandler
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        SeamlessGhostInjectQueue.drain();
    }
}
