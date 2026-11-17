package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Item.Name.ToolBoxColors;
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

public record ToolBoxColorsPayload(List<ToolBoxColors.Entry> entries) implements CustomPacketPayload
{
    public static final Type<ToolBoxColorsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "toolbox_colors"));

    public static final StreamCodec<ByteBuf, ToolBoxColorsPayload> CODEC = StreamCodec.of(ToolBoxColorsPayload::write, ToolBoxColorsPayload::read);

    private static void write(ByteBuf buf, ToolBoxColorsPayload payload)
    {
        VarInt.write(buf, payload.entries.size());
        for (ToolBoxColors.Entry entry : payload.entries)
        {
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.target());
            buf.writeInt(entry.rgb());
        }
    }

    private static ToolBoxColorsPayload read(ByteBuf buf)
    {
        int size = VarInt.read(buf);
        List<ToolBoxColors.Entry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            entries.add(new ToolBoxColors.Entry(ByteBufCodecs.STRING_UTF8.decode(buf), buf.readInt()));

        return new ToolBoxColorsPayload(entries);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
