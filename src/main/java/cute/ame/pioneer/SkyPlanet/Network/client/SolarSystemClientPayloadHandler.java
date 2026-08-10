package cute.ame.pioneer.SkyPlanet.Network.client;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Network.SolarSystemSyncPayload;

public final class SolarSystemClientPayloadHandler
{
    public static void handleSync(SolarSystemSyncPayload payload)
    {
        PioneerAPI.clearAll();
        payload.systems().forEach(PioneerAPI::registerSolarSystem);
        Pioneer.LOGGER.debug("[Pioneer] Client received {} solar system(s)", payload.systems().size());
    }
}
