package cute.ame.pioneer.Gas.Network.client;

import cute.ame.celsius.Fluid.Registry.SpeciesRegistry;
import cute.ame.pioneer.Gas.Network.GasSyncPayload;
import cute.ame.pioneer.Gas.Registry.GasRegistry;

public final class GasClientPayloadHandler
{
    public static void handleSync(GasSyncPayload payload)
    {
        SpeciesRegistry.replaceAll(payload.species());
        GasRegistry.replaceAll(payload.properties());
    }
}
