package cute.ame.pioneer.Seamless.Network;

import cute.ame.pioneer.Pioneer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TransitionSwapCuePayload(ResourceKey<Level> from, ResourceKey<Level> target) implements CustomPacketPayload
{
    public static final Type<TransitionSwapCuePayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "transition_swap_cue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransitionSwapCuePayload> CODEC =
        StreamCodec.composite(
            ResourceKey.streamCodec(net.minecraft.core.registries.Registries.DIMENSION), TransitionSwapCuePayload::from,
            ResourceKey.streamCodec(net.minecraft.core.registries.Registries.DIMENSION), TransitionSwapCuePayload::target,
            TransitionSwapCuePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
