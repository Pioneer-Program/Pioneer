package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RingPhotometry(
    float singleScatterAlbedo,
    float asymmetry,
    float backscatterFraction,
    float oppositionB0,
    float oppositionH,
    float planetShine
)
{
    public static final RingPhotometry DEFAULT = new RingPhotometry(0.6f, 0.6f, 0.35f, 1.0f, 0.02f, 0.25f);

    public static final MapCodec<RingPhotometry> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
        i.group(
            Codec.FLOAT.optionalFieldOf("single_scatter_albedo", 0.6f).forGetter(RingPhotometry::singleScatterAlbedo),
            Codec.FLOAT.optionalFieldOf("asymmetry", 0.6f).forGetter(RingPhotometry::asymmetry),
            Codec.FLOAT.optionalFieldOf("backscatter_fraction", 0.35f).forGetter(RingPhotometry::backscatterFraction),
            Codec.FLOAT.optionalFieldOf("opposition_b0", 1.0f).forGetter(RingPhotometry::oppositionB0),
            Codec.FLOAT.optionalFieldOf("opposition_h", 0.02f).forGetter(RingPhotometry::oppositionH),
            Codec.FLOAT.optionalFieldOf("planet_shine", 0.25f).forGetter(RingPhotometry::planetShine)
        ).apply(i, RingPhotometry::new)
    );
}
