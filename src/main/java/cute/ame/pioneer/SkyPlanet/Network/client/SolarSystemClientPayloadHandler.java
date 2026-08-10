package cute.ame.pioneer.SkyPlanet.Network.client;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Core.API.AuralithAPI;
import cute.ame.pioneer.SkyPlanet.Network.SolarSystemSyncPayload;

public final class SolarSystemClientPayloadHandler
{
    public static void handleSync(SolarSystemSyncPayload payload)
    {
        AuralithAPI.clearAll();
        payload.systems().forEach(AuralithAPI::registerSolarSystem);
        Pioneer.LOGGER.debug("[Auralith] Client received {} solar system(s)", payload.systems().size());
    }
}
