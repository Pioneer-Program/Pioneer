package cute.ame.pioneer.Frame.Network;

import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Pioneer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record FrameSyncPayload(boolean reset, int radius, int spacing, int y, List<Entry> upserts, int[] removals) implements CustomPacketPayload
{
    public static final Type<FrameSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "frame_sync"));

    public static final StreamCodec<FriendlyByteBuf, FrameSyncPayload> CODEC = StreamCodec.of(FrameSyncPayload::write, FrameSyncPayload::read);

    private static void writeEntry(FriendlyByteBuf buf, Entry e)
    {
        FrameMotion m = e.motion;
        buf.writeVarInt(e.id);
        buf.writeVarInt(e.cell);
        buf.writeLong(m.epoch);
        buf.writeDouble(m.px);
        buf.writeDouble(m.py);
        buf.writeDouble(m.pz);
        buf.writeDouble(m.vx);
        buf.writeDouble(m.vy);
        buf.writeDouble(m.vz);
        buf.writeDouble(m.ax);
        buf.writeDouble(m.ay);
        buf.writeDouble(m.az);
        buf.writeDouble(m.qx);
        buf.writeDouble(m.qy);
        buf.writeDouble(m.qz);
        buf.writeDouble(m.qw);
        buf.writeDouble(m.wx);
        buf.writeDouble(m.wy);
        buf.writeDouble(m.wz);
        buf.writeUtf(e.parent);
        buf.writeBoolean(e.fixed);
    }

    private static void write(FriendlyByteBuf buf, FrameSyncPayload p)
    {
        buf.writeBoolean(p.reset);
        buf.writeVarInt(p.radius);
        buf.writeVarInt(p.spacing);
        buf.writeVarInt(p.y);
        buf.writeVarInt(p.upserts.size());
        for (Entry e : p.upserts) writeEntry(buf, e);

        buf.writeVarIntArray(p.removals);
    }

    private static FrameSyncPayload read(FriendlyByteBuf buf)
    {
        boolean reset = buf.readBoolean();
        int radius = buf.readVarInt(), spacing = buf.readVarInt(), y = buf.readVarInt();
        int n = buf.readVarInt();
        List<Entry> upserts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) upserts.add(readEntry(buf));

        return new FrameSyncPayload(reset, radius, spacing, y, upserts, buf.readVarIntArray());
    }

    private static Entry readEntry(FriendlyByteBuf buf)
    {
        int id = buf.readVarInt(), cell = buf.readVarInt();
        long epoch = buf.readLong();
        FrameMotion m = new FrameMotion().set(epoch, buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        m.orient(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        m.spin(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new Entry(id, cell, m, buf.readUtf(), buf.readBoolean());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public record Entry(int id, int cell, FrameMotion motion, String parent, boolean fixed)
    {
    }
}
