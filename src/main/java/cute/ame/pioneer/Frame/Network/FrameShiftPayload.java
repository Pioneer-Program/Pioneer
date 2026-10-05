package cute.ame.pioneer.Frame.Network;

import cute.ame.pioneer.Pioneer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FrameShiftPayload(int tick, double dx, double dy, double dz, List<UUID> subLevels, int[] entities) implements CustomPacketPayload
{
    public static final Type<FrameShiftPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "frame_shift"));

    public static final StreamCodec<FriendlyByteBuf, FrameShiftPayload> CODEC = StreamCodec.of(FrameShiftPayload::write, FrameShiftPayload::read);

    private static void write(FriendlyByteBuf buf, FrameShiftPayload p)
    {
        buf.writeVarInt(p.tick);
        buf.writeDouble(p.dx);
        buf.writeDouble(p.dy);
        buf.writeDouble(p.dz);
        buf.writeVarInt(p.subLevels.size());
        for (UUID id : p.subLevels) buf.writeUUID(id);

        buf.writeVarIntArray(p.entities);
    }

    private static FrameShiftPayload read(FriendlyByteBuf buf)
    {
        int tick = buf.readVarInt();
        double dx = buf.readDouble(), dy = buf.readDouble(), dz = buf.readDouble();
        int n = buf.readVarInt();
        List<UUID> subLevels = new ArrayList<>(n);
        for (int i = 0; i < n; i++) subLevels.add(buf.readUUID());

        return new FrameShiftPayload(tick, dx, dy, dz, subLevels, buf.readVarIntArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
