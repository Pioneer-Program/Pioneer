package cute.ame.auralithpioneerinitiative.SkyPlanet.Network;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Network.client.SolarSystemClientPayloadHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Auralithpioneerinitiative.MODID)
public final class SolarSystemNetworking
{
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Auralithpioneerinitiative.MODID).versioned("1");

        registrar.commonToClient(SolarSystemSyncPayload.TYPE, SolarSystemSyncPayload.CODEC, (payload, context) -> context.enqueueWork(() -> SolarSystemClientPayloadHandler.handleSync(payload)));
    }
}
