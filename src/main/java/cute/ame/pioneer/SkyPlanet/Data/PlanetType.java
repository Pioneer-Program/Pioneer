package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum PlanetType
{
  ROCKY,
  GAS_GIANT,
  OCEAN,
  ICE,
  LAVA,
  TELLURIC;

  public static final Codec<PlanetType> CODEC = Codec.STRING.xmap(
    s -> PlanetType.valueOf(s.toUpperCase(Locale.ROOT)),
    e -> e.name().toLowerCase(Locale.ROOT)
  );

  public ResourceLocation defaultGeneratorId()
  {
    return ResourceLocation.fromNamespaceAndPath("pioneer", name().toLowerCase(Locale.ROOT));
  }
}
