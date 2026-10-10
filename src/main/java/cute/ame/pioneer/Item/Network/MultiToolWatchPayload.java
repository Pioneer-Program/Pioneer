package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record MultiToolWatchPayload(boolean watching, long pos) implements CustomPacketPayload
{
    public static final Type<MultiToolWatchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_watch"));

    public static final StreamCodec<ByteBuf, MultiToolWatchPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, MultiToolWatchPayload::watching, ByteBufCodecs.VAR_LONG, MultiToolWatchPayload::pos, MultiToolWatchPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
