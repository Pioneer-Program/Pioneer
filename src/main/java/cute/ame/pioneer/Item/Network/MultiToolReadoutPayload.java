package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Core.Readout.Readout;
import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record MultiToolReadoutPayload(long pos, List<Line> lines) implements CustomPacketPayload
{
    public static final Type<MultiToolReadoutPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_readout"));

    public static final StreamCodec<ByteBuf, MultiToolReadoutPayload> CODEC = StreamCodec.of(MultiToolReadoutPayload::write, MultiToolReadoutPayload::read);

    public static MultiToolReadoutPayload of(long pos, Readout readout)
    {
        int size = readout.size();
        List<Line> lines = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            lines.add(new Line(readout.key(i), readout.kind(i), (byte) readout.unit(i).ordinal(), readout.value(i), readout.text(i)));

        return new MultiToolReadoutPayload(pos, lines);
    }

    private static void write(ByteBuf buf, MultiToolReadoutPayload payload)
    {
        buf.writeLong(payload.pos);
        VarInt.write(buf, payload.lines.size());

        for (Line line : payload.lines)
        {
            ByteBufCodecs.STRING_UTF8.encode(buf, line.key);
            buf.writeByte(line.kind);
            buf.writeByte(line.unit);

            if (line.kind == Readout.NUMBER) buf.writeDouble(line.value);
            else ByteBufCodecs.STRING_UTF8.encode(buf, line.text);
        }
    }

    private static MultiToolReadoutPayload read(ByteBuf buf)
    {
        long pos = buf.readLong();
        int size = VarInt.read(buf);
        List<Line> lines = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
        {
            String key = ByteBufCodecs.STRING_UTF8.decode(buf);
            byte kind = buf.readByte();
            byte unit = buf.readByte();

            if (kind == Readout.NUMBER) lines.add(new Line(key, kind, unit, buf.readDouble(), ""));
            else lines.add(new Line(key, kind, unit, 0.0, ByteBufCodecs.STRING_UTF8.decode(buf)));
        }

        return new MultiToolReadoutPayload(pos, lines);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public record Line(String key, byte kind, byte unit, double value, String text)
    {
    }
}
