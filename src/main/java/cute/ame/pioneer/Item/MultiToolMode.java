package cute.ame.pioneer.Item;

import com.mojang.serialization.Codec;
import cute.ame.pioneer.Pioneer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum MultiToolMode implements StringRepresentable
{
    CONFIGURE("configure"),
    NAME("name"),
    ROTATE("rotate"),

    ;

    public static final Codec<MultiToolMode> CODEC = StringRepresentable.fromEnum(MultiToolMode::values);
    private static final MultiToolMode[] VALUES = values();
    public static final StreamCodec<ByteBuf, MultiToolMode> STREAM_CODEC = ByteBufCodecs.BYTE.map(index -> byIndex(index), mode -> (byte) mode.ordinal());

    private final String name;
    private final ResourceLocation icon;
    private final Component title;

    MultiToolMode(String name)
    {
        this.name = name;
        this.icon = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "textures/gui/multitool/" + name + ".png");
        this.title = Component.translatable("multitool.pioneer.mode." + name);
    }

    public static int count()
    {
        return VALUES.length;
    }

    public static MultiToolMode byIndex(int index)
    {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    public MultiToolMode cycle(boolean forward)
    {
        return byIndex(ordinal() + (forward ? 1 : -1));
    }

    public ResourceLocation icon()
    {
        return icon;
    }

    public Component title()
    {
        return title;
    }

    @Override
    public @NotNull String getSerializedName()
    {
        return name;
    }
}
