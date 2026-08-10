package cute.ame.pioneer.SkyPlanet.Network;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Network.client.SolarSystemClientPayloadHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class SolarSystemNetworking
{
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Pioneer.MODID).versioned("1");

        registrar.commonToClient(SolarSystemSyncPayload.TYPE, SolarSystemSyncPayload.CODEC, (payload, context) -> context.enqueueWork(() -> SolarSystemClientPayloadHandler.handleSync(payload)));
    }
}
