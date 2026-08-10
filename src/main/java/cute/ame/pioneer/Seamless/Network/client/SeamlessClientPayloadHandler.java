package cute.ame.pioneer.Seamless.Network.client;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Seamless.Client.SeamlessGhostAnchors;
import cute.ame.pioneer.Seamless.Client.SeamlessGhostChunkStore;
import cute.ame.pioneer.Seamless.Client.SeamlessGhostDimensionTypes;
import cute.ame.pioneer.Seamless.Client.SeamlessGhostLevelBuilder;
import cute.ame.pioneer.Seamless.Network.ChunkStreamPayload;

import cute.ame.pioneer.Seamless.Network.PreloadCancelPayload;
import cute.ame.pioneer.Seamless.Network.PreloadDimensionPayload;
import cute.ame.pioneer.Seamless.Network.TransitionCompletePayload;
import cute.ame.pioneer.Seamless.SeamlessLevelRegistry;
import cute.ame.pioneer.Seamless.SeamlessTransitionState;

public final class SeamlessClientPayloadHandler
{
    public static void handlePreload(PreloadDimensionPayload payload)
    {
        SeamlessTransitionState.beginTransition(payload.from(), payload.target());
        SeamlessGhostDimensionTypes.remember(payload.target(), payload.targetDimensionType());
        SeamlessGhostAnchors.remember(payload.target(), payload.anchorPos());
        Pioneer.LOGGER.debug("[Auralith] Client received preload hint for {} (anchor {})", payload.target().location(), payload.anchorPos());
    }

    public static void handleCancel(PreloadCancelPayload payload)
    {
        Pioneer.LOGGER.debug("[Auralith] Client received preload CANCEL for {}", payload.target().location());
        SeamlessLevelRegistry.release(payload.target());
        SeamlessTransitionState.forgetInvolving(payload.target());
        SeamlessGhostDimensionTypes.forget(payload.target());
        SeamlessGhostAnchors.forget(payload.target());
    }

    public static void handleComplete(TransitionCompletePayload payload)
    {
        Pioneer.LOGGER.debug("[Auralith] Client transition SETTLED {} -> {}", payload.from().location(), payload.target().location());
        SeamlessTransitionState.endTransition(payload.from(), payload.target());
        SeamlessLevelRegistry.release(payload.from());
        SeamlessLevelRegistry.release(payload.target());
    }

    public static void handleChunkStream(ChunkStreamPayload payload)
    {
        SeamlessGhostChunkStore.store(payload.target(), payload.pos(), payload.chunkPacketBytes());
        Pioneer.LOGGER.debug("[Auralith] Client stored ghost chunk {} for {} ({} bytes)", payload.pos(), payload.target().location(), payload.chunkPacketBytes().length);
        SeamlessGhostLevelBuilder.ensureAndInject(payload.target(), payload.pos());
    }
}