package cute.ame.auralithpioneerinitiative.Seamless.Network;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record PreloadDimensionPayload(ResourceKey<Level> from, ResourceKey<Level> target, BlockPos anchorPos, ResourceKey<DimensionType> targetDimensionType) implements CustomPacketPayload
{
    public static final Type<PreloadDimensionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Auralithpioneerinitiative.MODID, "preload_dimension"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PreloadDimensionPayload> CODEC =
        StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), PreloadDimensionPayload::from,
            ResourceKey.streamCodec(Registries.DIMENSION), PreloadDimensionPayload::target,
            BlockPos.STREAM_CODEC, PreloadDimensionPayload::anchorPos,
            ResourceKey.streamCodec(Registries.DIMENSION_TYPE), PreloadDimensionPayload::targetDimensionType,
            PreloadDimensionPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}