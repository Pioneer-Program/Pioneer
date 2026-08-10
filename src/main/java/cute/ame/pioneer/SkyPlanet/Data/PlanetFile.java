package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record PlanetFile(PlanetDefinition base, List<ResourceLocation> moons)
{
    private static final Codec<List<ResourceLocation>> MOON_REFS = ResourceLocation.CODEC.listOf();

    public static final Codec<PlanetFile> CODEC = RecordCodecBuilder.create(i -> i.group(
        PlanetDefinition.CORE_CODEC.forGetter(PlanetFile::base),
        MOON_REFS.optionalFieldOf("moons", List.of()).forGetter(PlanetFile::moons)
    ).apply(i, PlanetFile::new));
}
