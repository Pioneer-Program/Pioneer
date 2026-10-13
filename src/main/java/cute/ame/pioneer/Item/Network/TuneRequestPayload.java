package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record TuneRequestPayload(long pos) implements CustomPacketPayload
{
    public static final Type<TuneRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "tune_request"));

    public static final StreamCodec<ByteBuf, TuneRequestPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, TuneRequestPayload::pos, TuneRequestPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
