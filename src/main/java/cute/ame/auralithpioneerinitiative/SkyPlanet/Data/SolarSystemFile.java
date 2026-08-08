package cute.ame.auralithpioneerinitiative.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record SolarSystemFile(SunDefinition sun, List<ResourceLocation> planets, Optional<ResourceLocation> spaceDimension)
{
    private static final Codec<List<ResourceLocation>> PLANET_REFS = ResourceLocation.CODEC.listOf();

    public static final Codec<SolarSystemFile> CODEC = RecordCodecBuilder.create(i -> i.group(
        SunDefinition.CODEC.fieldOf("sun").forGetter(SolarSystemFile::sun),
        PLANET_REFS.fieldOf("planets").forGetter(SolarSystemFile::planets),
        ResourceLocation.CODEC.optionalFieldOf("space_dimension").forGetter(SolarSystemFile::spaceDimension)
    ).apply(i, SolarSystemFile::new));
}
