package cute.ame.pioneer.Frame.Network;

import cute.ame.pioneer.Frame.Client.ClientFrames;
import cute.ame.pioneer.Frame.Client.FrameShiftClient;
import cute.ame.pioneer.Pioneer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class FrameNetworking
{
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Pioneer.MODID).versioned("1");
        registrar.commonToClient(FrameShiftPayload.TYPE, FrameShiftPayload.CODEC, (payload, context) -> FrameShiftClient.apply(payload));
        registrar.commonToClient(FrameSyncPayload.TYPE, FrameSyncPayload.CODEC, (payload, context) -> ClientFrames.apply(payload));
    }
}
