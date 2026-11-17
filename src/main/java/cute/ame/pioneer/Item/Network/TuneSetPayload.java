package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record TuneSetPayload(long pos, int option, double value) implements CustomPacketPayload
{
    public static final int TOGGLE = -1;

    public static final Type<TuneSetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "tune_set"));

    public static final StreamCodec<ByteBuf, TuneSetPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, TuneSetPayload::pos, ByteBufCodecs.VAR_INT, TuneSetPayload::option, ByteBufCodecs.DOUBLE, TuneSetPayload::value, TuneSetPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
