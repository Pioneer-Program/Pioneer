package cute.ame.pioneer.SkyPlanet.Network.client;

import cute.ame.pioneer.SkyPlanet.Network.GasSyncPayload;
import cute.ame.pioneer.SkyPlanet.Registry.GasRegistry;

public final class GasClientPayloadHandler
{
    public static void handleSync(GasSyncPayload payload)
    {
        GasRegistry.replaceAll(payload.gases());
    }
}
