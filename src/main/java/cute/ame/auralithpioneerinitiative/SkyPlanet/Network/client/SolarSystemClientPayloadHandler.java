package cute.ame.auralithpioneerinitiative.SkyPlanet.Network.client;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Network.SolarSystemSyncPayload;

public final class SolarSystemClientPayloadHandler
{
    public static void handleSync(SolarSystemSyncPayload payload)
    {
        AuralithAPI.clearAll();
        payload.systems().forEach(AuralithAPI::registerSolarSystem);
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Client received {} solar system(s)", payload.systems().size());
    }
}
