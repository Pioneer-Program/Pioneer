package cute.ame.auralithpioneerinitiative.Seamless.Network;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record PreloadCancelPayload(ResourceKey<Level> target) implements CustomPacketPayload
{
    public static final Type<PreloadCancelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Auralithpioneerinitiative.MODID, "preload_cancel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PreloadCancelPayload> CODEC = StreamCodec.composite(ResourceKey.streamCodec(Registries.DIMENSION), PreloadCancelPayload::target, PreloadCancelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}