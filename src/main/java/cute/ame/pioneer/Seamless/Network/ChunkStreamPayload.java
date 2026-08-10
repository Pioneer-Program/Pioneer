package cute.ame.pioneer.Seamless.Network;

import cute.ame.pioneer.Pioneer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
public record ChunkStreamPayload(ResourceKey<Level> target, ChunkPos pos, byte[] chunkPacketBytes) implements CustomPacketPayload
{
    public static final Type<ChunkStreamPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "chunk_stream"));

    private static final StreamCodec<RegistryFriendlyByteBuf, ChunkPos> CHUNK_POS_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, ChunkPos::toLong, ChunkPos::new);

    private static final int MAX_CHUNK_PACKET_BYTES = 2 * 1024 * 1024;

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkStreamPayload> CODEC = StreamCodec.composite(ResourceKey.streamCodec(Registries.DIMENSION), ChunkStreamPayload::target, CHUNK_POS_CODEC, ChunkStreamPayload::pos, ByteBufCodecs.byteArray(MAX_CHUNK_PACKET_BYTES), ChunkStreamPayload::chunkPacketBytes, ChunkStreamPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
