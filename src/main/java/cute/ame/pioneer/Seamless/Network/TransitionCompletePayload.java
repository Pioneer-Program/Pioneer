package cute.ame.pioneer.Seamless.Network;

import cute.ame.pioneer.Pioneer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record TransitionCompletePayload(ResourceKey<Level> from, ResourceKey<Level> target) implements CustomPacketPayload
{
    public static final Type<TransitionCompletePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "transition_complete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransitionCompletePayload> CODEC = StreamCodec.composite(ResourceKey.streamCodec(Registries.DIMENSION), TransitionCompletePayload::from, ResourceKey.streamCodec(Registries.DIMENSION), TransitionCompletePayload::target, TransitionCompletePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}