package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SurfaceMapping
(
    float originLatitude,
    float originLongitude,
    float surfaceScale,
    boolean swapAxes
)
{
    public static final SurfaceMapping DEFAULT = new SurfaceMapping(0.0f, 0.0f, 1.0f, false);

    public static final MapCodec<SurfaceMapping> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
        i.group(
            Codec.FLOAT.optionalFieldOf("origin_latitude", 0.0f).forGetter(SurfaceMapping::originLatitude),
            Codec.FLOAT.optionalFieldOf("origin_longitude", 0.0f).forGetter(SurfaceMapping::originLongitude),
            Codec.FLOAT.optionalFieldOf("surface_scale", 1.0f).forGetter(SurfaceMapping::surfaceScale),
            Codec.BOOL.optionalFieldOf("swap_surface_axes", false).forGetter(SurfaceMapping::swapAxes)
        ).apply(i, SurfaceMapping::new)
    );
}
