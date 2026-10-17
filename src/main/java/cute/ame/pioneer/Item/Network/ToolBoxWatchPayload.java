package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record ToolBoxWatchPayload(boolean watching, long pos) implements CustomPacketPayload
{
    public static final Type<ToolBoxWatchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "toolbox_watch"));

    public static final StreamCodec<ByteBuf, ToolBoxWatchPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, ToolBoxWatchPayload::watching, ByteBufCodecs.VAR_LONG, ToolBoxWatchPayload::pos, ToolBoxWatchPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
