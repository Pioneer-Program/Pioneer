package cute.ame.pioneer.SkyPlanet.Network;

import com.mojang.serialization.Codec;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.GasDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record GasSyncPayload(Map<ResourceLocation, GasDefinition> gases) implements CustomPacketPayload
{
    public static final Type<GasSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "gas_sync"));

    private static final Codec<Map<ResourceLocation, GasDefinition>> GASES_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, GasDefinition.CODEC);
    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, GasDefinition>> GASES_STREAM_CODEC = ByteBufCodecs.fromCodec(GASES_CODEC).cast();
    public static final StreamCodec<FriendlyByteBuf, GasSyncPayload> CODEC = StreamCodec.composite(GASES_STREAM_CODEC, GasSyncPayload::gases, GasSyncPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
