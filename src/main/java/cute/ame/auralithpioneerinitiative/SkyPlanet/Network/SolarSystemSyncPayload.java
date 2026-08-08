package cute.ame.auralithpioneerinitiative.SkyPlanet.Network;

import com.mojang.serialization.Codec;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record SolarSystemSyncPayload(Map<ResourceLocation, SolarSystemDefinition> systems) implements CustomPacketPayload
{
    public static final Type<SolarSystemSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Auralithpioneerinitiative.MODID, "solar_system_sync"));

    private static final Codec<Map<ResourceLocation, SolarSystemDefinition>> SYSTEMS_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, SolarSystemDefinition.CODEC);

    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, SolarSystemDefinition>> SYSTEMS_STREAM_CODEC = ByteBufCodecs.fromCodec(SYSTEMS_CODEC).cast();

    public static final StreamCodec<FriendlyByteBuf, SolarSystemSyncPayload> CODEC = StreamCodec.composite(SYSTEMS_STREAM_CODEC, SolarSystemSyncPayload::systems, SolarSystemSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
