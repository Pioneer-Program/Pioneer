package cute.ame.pioneer.Item.Network;

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

public record NameLabelsPayload(boolean replace, List<Entry> entries) implements CustomPacketPayload
{
    public static final Type<NameLabelsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "name_labels"));

    public static final StreamCodec<ByteBuf, NameLabelsPayload> CODEC = StreamCodec.of(NameLabelsPayload::write, NameLabelsPayload::read);

    private static void write(ByteBuf buf, NameLabelsPayload payload)
    {
        buf.writeBoolean(payload.replace);
        VarInt.write(buf, payload.entries.size());

        for (Entry entry : payload.entries)
        {
            buf.writeLong(entry.pos());
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.label());
        }
    }

    private static NameLabelsPayload read(ByteBuf buf)
    {
        boolean replace = buf.readBoolean();
        int size = VarInt.read(buf);
        List<Entry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) entries.add(new Entry(buf.readLong(), ByteBufCodecs.STRING_UTF8.decode(buf)));

        return new NameLabelsPayload(replace, entries);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public record Entry(long pos, String label)
    {
    }
}
