package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Core.Configure.TuneOption;
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

public record TuneOptionsPayload(long pos, List<TuneOption> options) implements CustomPacketPayload
{
    public static final Type<TuneOptionsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "tune_options"));

    public static final StreamCodec<ByteBuf, TuneOptionsPayload> CODEC = StreamCodec.of(TuneOptionsPayload::write, TuneOptionsPayload::read);

    private static void write(ByteBuf buf, TuneOptionsPayload payload)
    {
        buf.writeLong(payload.pos);
        VarInt.write(buf, payload.options.size());
        for (TuneOption option : payload.options)
        {
            ByteBufCodecs.STRING_UTF8.encode(buf, option.key());
            buf.writeByte(option.kind());
            buf.writeDouble(option.value());
            buf.writeDouble(option.min());
            buf.writeDouble(option.max());
            buf.writeDouble(option.step());
            buf.writeByte(option.unit());
            buf.writeBoolean(option.primary());
            VarInt.write(buf, option.choices().size());
            for (String choice : option.choices()) ByteBufCodecs.STRING_UTF8.encode(buf, choice);

            VarInt.write(buf, option.icons().size());
            for (String icon : option.icons()) ByteBufCodecs.STRING_UTF8.encode(buf, icon);
        }
    }

    private static TuneOptionsPayload read(ByteBuf buf)
    {
        long pos = buf.readLong();
        int size = VarInt.read(buf);
        List<TuneOption> options = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            String key = ByteBufCodecs.STRING_UTF8.decode(buf);
            byte kind = buf.readByte();
            double value = buf.readDouble();
            double min = buf.readDouble();
            double max = buf.readDouble();
            double step = buf.readDouble();
            byte unit = buf.readByte();
            boolean primary = buf.readBoolean();
            int count = VarInt.read(buf);
            List<String> choices = new ArrayList<>(count);
            for (int c = 0; c < count; c++) choices.add(ByteBufCodecs.STRING_UTF8.decode(buf));

            int iconCount = VarInt.read(buf);
            List<String> icons = new ArrayList<>(iconCount);
            for (int c = 0; c < iconCount; c++) icons.add(ByteBufCodecs.STRING_UTF8.decode(buf));

            options.add(new TuneOption(key, kind, value, min, max, step, unit, List.copyOf(choices), List.copyOf(icons), primary));
        }

        return new TuneOptionsPayload(pos, options);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
