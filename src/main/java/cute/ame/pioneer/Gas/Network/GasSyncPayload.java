package cute.ame.pioneer.Gas.Network;

import com.mojang.serialization.Codec;
import cute.ame.celsius.Fluid.Data.SpeciesDefinition;
import cute.ame.pioneer.Gas.Data.GasProperties;
import cute.ame.pioneer.Pioneer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record GasSyncPayload(Map<ResourceLocation, SpeciesDefinition> species, Map<ResourceLocation, GasProperties> properties) implements CustomPacketPayload
{
    public static final Type<GasSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "gas_sync"));

    private static final Codec<Map<ResourceLocation, SpeciesDefinition>> SPECIES_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, SpeciesDefinition.CODEC);
    private static final Codec<Map<ResourceLocation, GasProperties>> PROPERTIES_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, GasProperties.CODEC);

    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, SpeciesDefinition>> SPECIES_STREAM_CODEC = ByteBufCodecs.fromCodec(SPECIES_CODEC).cast();
    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, GasProperties>> PROPERTIES_STREAM_CODEC = ByteBufCodecs.fromCodec(PROPERTIES_CODEC).cast();

    public static final StreamCodec<FriendlyByteBuf, GasSyncPayload> CODEC = StreamCodec.composite(
        SPECIES_STREAM_CODEC, GasSyncPayload::species,
        PROPERTIES_STREAM_CODEC, GasSyncPayload::properties,
        GasSyncPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
