package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record MultiToolModePayload(boolean forward) implements CustomPacketPayload
{
    public static final Type<MultiToolModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_mode"));

    public static final StreamCodec<ByteBuf, MultiToolModePayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, MultiToolModePayload::forward, MultiToolModePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
