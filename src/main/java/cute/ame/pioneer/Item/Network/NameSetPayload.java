package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Item.Name.NameServer;
import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record NameSetPayload(long pos, String label) implements CustomPacketPayload
{
    public static final Type<NameSetPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "name_set"));

    public static final StreamCodec<ByteBuf, NameSetPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, NameSetPayload::pos, ByteBufCodecs.stringUtf8(NameServer.MAX_LENGTH * 4), NameSetPayload::label, NameSetPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
