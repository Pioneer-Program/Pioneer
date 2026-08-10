package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.Core.Render.Cache.PlanetTextureManager;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Optional;

public record PlanetDefinition(
    ResourceLocation id,
    float size,
    float axialRotationSpeed,
    float axialTilt,
    OrbitDefinition orbit,
    Optional<ProceduralPlanetConfig> procedural,
    Optional<AtmosphereDefinition> atmosphere,
    Optional<CloudsDefinition> clouds,
    Optional<RingDefinition> rings,
    List<PlanetDefinition> moons,
    Optional<ResourceLocation> dimension,
    float gravity
)
{
  public static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
  private static final CubemapTextures MISSING_CUBEMAP = new CubemapTextures(MISSING, MISSING, MISSING, MISSING, MISSING, MISSING);
  private static final float SPACE_WORLD_SCALE = 1.0f;
  private static final double APPROACH_SIZE_MARGIN = 300.0;
  private static final long TICKS_PER_DAY = 24000L;

  public static final MapCodec<PlanetDefinition> CORE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ResourceLocation.CODEC.optionalFieldOf("id", MISSING).forGetter(PlanetDefinition::id),
      Codec.FLOAT.fieldOf("size").forGetter(PlanetDefinition::size),
      Codec.FLOAT.optionalFieldOf("axial_rotation_speed", 1.0f).forGetter(PlanetDefinition::axialRotationSpeed),
      Codec.FLOAT.optionalFieldOf("axial_tilt", 0.0f).forGetter(PlanetDefinition::axialTilt),
      OrbitDefinition.CODEC.fieldOf("orbit").forGetter(PlanetDefinition::orbit),
      ProceduralPlanetConfig.CODEC.optionalFieldOf("procedural").forGetter(PlanetDefinition::procedural),
      AtmosphereDefinition.CODEC.optionalFieldOf("atmosphere").forGetter(PlanetDefinition::atmosphere),
      CloudsDefinition.CODEC.optionalFieldOf("clouds").forGetter(PlanetDefinition::clouds),
      RingDefinition.CODEC.optionalFieldOf("rings").forGetter(PlanetDefinition::rings),
      ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(PlanetDefinition::dimension),
      Codec.FLOAT.optionalFieldOf("gravity", 1.0f).forGetter(PlanetDefinition::gravity)
  ).apply(i, (id, size, rot, tilt, orbit, proc, atmo, clouds, rings, dim, grav) ->
      new PlanetDefinition(id, size, rot, tilt, orbit, proc, atmo, clouds, rings, List.of(), dim, grav)));

  private static final Codec<List<PlanetDefinition>> INLINE_MOONS = Codec.lazyInitialized(() -> PlanetDefinition.CODEC).listOf();

  public static final Codec<PlanetDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
      CORE_CODEC.forGetter(p -> p),
      INLINE_MOONS.optionalFieldOf("moons", List.of()).forGetter(PlanetDefinition::moons)
  ).apply(i, PlanetDefinition::withMoons));

  public PlanetDefinition withMoons(List<PlanetDefinition> newMoons)
  {
    return newMoons == moons ? this
        : new PlanetDefinition(id, size, axialRotationSpeed, axialTilt, orbit, procedural, atmosphere, clouds, rings, newMoons, dimension, gravity);
  }

  public PlanetDefinition withId(ResourceLocation newId)
  {
    return newId.equals(id) ? this
        : new PlanetDefinition(newId, size, axialRotationSpeed, axialTilt, orbit, procedural, atmosphere, clouds, rings, moons, dimension, gravity);
  }

  public CubemapTextures resolveTexture()
  {
    return procedural.map(cfg -> PlanetTextureManager.getOrGenerate(cfg, id.toString())).orElse(MISSING_CUBEMAP);
  }

  public double[] currentWorldPosition(long absoluteTick)
  {
    double angle = orbit.computeAngle(absoluteTick, 0f);
    double radius = orbit.computeCurrentRadius(angle);
    float[] pos = orbit.compute3DPosition(angle, radius, SPACE_WORLD_SCALE);
    return new double[]{ pos[0], pos[1], pos[2] };
  }

  public double approachRadius()
  {
    return size + APPROACH_SIZE_MARGIN;
  }

  public Quaternionf computeTrueRotation(long tick, float partialTick)
  {
    double phase = ((tick + partialTick) / (double) (axialRotationSpeed * TICKS_PER_DAY)) % 1.0;
    float spinAngle = (float) (phase * 2.0 * Math.PI);
    float tiltRad = (float) Math.toRadians(axialTilt);
    return new Quaternionf().rotationZ(tiltRad).mul(new Quaternionf().rotationY(spinAngle));
  }
}
