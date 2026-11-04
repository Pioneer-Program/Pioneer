package cute.ame.pioneer.Frame.Network;

import cute.ame.pioneer.Pioneer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FrameShiftAckPayload() implements CustomPacketPayload
{
    public static final FrameShiftAckPayload INSTANCE = new FrameShiftAckPayload();
    public static final Type<FrameShiftAckPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "frame_shift_ack"));
    public static final StreamCodec<FriendlyByteBuf, FrameShiftAckPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
