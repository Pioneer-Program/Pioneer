package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SurfaceMapping
(
    int homeFace,
    float homeU,
    float homeV,
    float surfaceScale,
    float verticalScale
)
{
    public static final SurfaceMapping DEFAULT = new SurfaceMapping(0, 0.0f, 0.0f, 1.0f, 1.0f);

    public static final MapCodec<SurfaceMapping> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
        i.group(
            Codec.intRange(0, 5).optionalFieldOf("home_face", 0).forGetter(SurfaceMapping::homeFace),
            Codec.floatRange(-1.0f, 1.0f).optionalFieldOf("home_u", 0.0f).forGetter(SurfaceMapping::homeU),
            Codec.floatRange(-1.0f, 1.0f).optionalFieldOf("home_v", 0.0f).forGetter(SurfaceMapping::homeV),
            Codec.FLOAT.optionalFieldOf("surface_scale", 1.0f).forGetter(SurfaceMapping::surfaceScale),
            Codec.FLOAT.optionalFieldOf("vertical_scale", 1.0f).forGetter(SurfaceMapping::verticalScale)
        ).apply(i, SurfaceMapping::new)
    );
}