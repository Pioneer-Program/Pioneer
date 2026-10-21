package cute.ame.pioneer.Frame.Network;

import cute.ame.pioneer.Core.Frame.FrameShift;
import cute.ame.pioneer.Pioneer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FrameShiftPayload(int tick, double dx, double dy, double dz, double qx, double qy, double qz, double qw, List<UUID> subLevels, int[] entities, int riders) implements CustomPacketPayload
{
    public static final Type<FrameShiftPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "frame_shift"));

    public static final StreamCodec<FriendlyByteBuf, FrameShiftPayload> CODEC = StreamCodec.of(FrameShiftPayload::write, FrameShiftPayload::read);

    public static FrameShiftPayload of(int tick, FrameShift shift, List<UUID> subLevels, int[] entities)
    {
        return of(tick, shift, subLevels, entities, 0);
    }

    public static FrameShiftPayload of(int tick, FrameShift shift, List<UUID> subLevels, int[] entities, int riders)
    {
        if (!shift.rotated)
            return new FrameShiftPayload(tick, shift.tx, shift.ty, shift.tz, 0.0, 0.0, 0.0, 1.0, subLevels, entities, riders);

        return new FrameShiftPayload(tick, shift.tx, shift.ty, shift.tz, shift.qx, shift.qy, shift.qz, shift.qw, subLevels, entities, riders);
    }

    private static void write(FriendlyByteBuf buf, FrameShiftPayload p)
    {
        buf.writeVarInt(p.tick);
        buf.writeDouble(p.dx);
        buf.writeDouble(p.dy);
        buf.writeDouble(p.dz);
        boolean rotated = p.rotated();
        buf.writeBoolean(rotated);
        if (rotated)
        {
            buf.writeDouble(p.qx);
            buf.writeDouble(p.qy);
            buf.writeDouble(p.qz);
            buf.writeDouble(p.qw);
        }

        buf.writeVarInt(p.subLevels.size());
        for (UUID id : p.subLevels) buf.writeUUID(id);

        buf.writeVarIntArray(p.entities);
        buf.writeVarInt(p.riders);
    }

    private static FrameShiftPayload read(FriendlyByteBuf buf)
    {
        int tick = buf.readVarInt();
        double dx = buf.readDouble(), dy = buf.readDouble(), dz = buf.readDouble();
        double qx = 0.0, qy = 0.0, qz = 0.0, qw = 1.0;
        if (buf.readBoolean())
        {
            qx = buf.readDouble();
            qy = buf.readDouble();
            qz = buf.readDouble();
            qw = buf.readDouble();
        }

        int n = buf.readVarInt();
        List<UUID> subLevels = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) subLevels.add(buf.readUUID());

        int[] entities = buf.readVarIntArray();
        return new FrameShiftPayload(tick, dx, dy, dz, qx, qy, qz, qw, subLevels, entities, Math.min(buf.readVarInt(), entities.length));
    }

    public boolean rotated()
    {
        return qx != 0.0 || qy != 0.0 || qz != 0.0;
    }

    public FrameShift shift(FrameShift out)
    {
        out.translation(dx, dy, dz, 0.0, 0.0, 0.0);
        if (!rotated()) return out;

        out.qx = qx;
        out.qy = qy;
        out.qz = qz;
        out.qw = qw;
        out.rotated = true;
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
