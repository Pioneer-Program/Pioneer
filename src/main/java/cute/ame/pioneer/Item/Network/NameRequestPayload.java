package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record NameRequestPayload() implements CustomPacketPayload
{
    public static final NameRequestPayload INSTANCE = new NameRequestPayload();

    public static final Type<NameRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "name_request"));

    public static final StreamCodec<ByteBuf, NameRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
