package cute.ame.pioneer.Seamless.Network;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Seamless.Network.client.SeamlessClientPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SeamlessNetworking
{
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Pioneer.MODID).versioned("1");

        registrar.playToClient(
            PreloadDimensionPayload.TYPE,
            PreloadDimensionPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> SeamlessClientPayloadHandler.handlePreload(payload))
        );

        registrar.playToClient(
            PreloadCancelPayload.TYPE,
            PreloadCancelPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> SeamlessClientPayloadHandler.handleCancel(payload))
        );

        registrar.playToClient(
            TransitionCompletePayload.TYPE,
            TransitionCompletePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> SeamlessClientPayloadHandler.handleComplete(payload))
        );

        registrar.playToClient(
            ChunkStreamPayload.TYPE,
            ChunkStreamPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> SeamlessClientPayloadHandler.handleChunkStream(payload))
        );
    }
}