package cute.ame.auralithpioneerinitiative.Seamless.Network.client;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostAnchors;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostChunkStore;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostDimensionTypes;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostLevelBuilder;
import cute.ame.auralithpioneerinitiative.Seamless.Network.ChunkStreamPayload;

import cute.ame.auralithpioneerinitiative.Seamless.Network.PreloadCancelPayload;
import cute.ame.auralithpioneerinitiative.Seamless.Network.PreloadDimensionPayload;
import cute.ame.auralithpioneerinitiative.Seamless.Network.TransitionCompletePayload;
import cute.ame.auralithpioneerinitiative.Seamless.SeamlessLevelRegistry;
import cute.ame.auralithpioneerinitiative.Seamless.SeamlessTransitionState;
import net.minecraft.client.Screenshot;

public final class SeamlessClientPayloadHandler
{
    public static void handlePreload(PreloadDimensionPayload payload)
    {
        SeamlessTransitionState.beginTransition(payload.from(), payload.target());
        SeamlessGhostDimensionTypes.remember(payload.target(), payload.targetDimensionType());
        SeamlessGhostAnchors.remember(payload.target(), payload.anchorPos());
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Client received preload hint for {} (anchor {})", payload.target().location(), payload.anchorPos());
    }

    public static void handleCancel(PreloadCancelPayload payload)
    {
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Client received preload CANCEL for {}", payload.target().location());
        SeamlessLevelRegistry.release(payload.target());
        SeamlessTransitionState.forgetInvolving(payload.target());
        SeamlessGhostDimensionTypes.forget(payload.target());
        SeamlessGhostAnchors.forget(payload.target());
    }

    public static void handleComplete(TransitionCompletePayload payload)
    {
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Client transition SETTLED {} -> {}", payload.from().location(), payload.target().location());
        SeamlessTransitionState.endTransition(payload.from(), payload.target());
        SeamlessLevelRegistry.release(payload.from());
        SeamlessLevelRegistry.release(payload.target());
    }

    public static void handleChunkStream(ChunkStreamPayload payload)
    {
        SeamlessGhostChunkStore.store(payload.target(), payload.pos(), payload.chunkPacketBytes());
        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Client stored ghost chunk {} for {} ({} bytes)", payload.pos(), payload.target().location(), payload.chunkPacketBytes().length);
        SeamlessGhostLevelBuilder.ensureAndInject(payload.target(), payload.pos());
    }
}