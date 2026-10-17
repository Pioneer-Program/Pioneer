package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Item.Name.MultiToolColors;
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

public record MultiToolColorsPayload(List<MultiToolColors.Entry> entries) implements CustomPacketPayload
{
    public static final Type<MultiToolColorsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "multitool_colors"));

    public static final StreamCodec<ByteBuf, MultiToolColorsPayload> CODEC = StreamCodec.of(MultiToolColorsPayload::write, MultiToolColorsPayload::read);

    private static void write(ByteBuf buf, MultiToolColorsPayload payload)
    {
        VarInt.write(buf, payload.entries.size());
        for (MultiToolColors.Entry entry : payload.entries)
        {
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.target());
            buf.writeInt(entry.rgb());
        }
    }

    private static MultiToolColorsPayload read(ByteBuf buf)
    {
        int size = VarInt.read(buf);
        List<MultiToolColors.Entry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            entries.add(new MultiToolColors.Entry(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readInt()));

        return new MultiToolColorsPayload(entries);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
