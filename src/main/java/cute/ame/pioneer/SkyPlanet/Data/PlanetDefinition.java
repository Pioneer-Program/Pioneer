package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.pioneer.Core.Render.Cache.PlanetTextureManager;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.SkyPlanet.Orbit.OrbitalElements;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    float gravity,
    float surfaceLatitude,
    float surfaceLongitude
)
{
  public static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
  private static final CubemapTextures MISSING_CUBEMAP = new CubemapTextures(MISSING, MISSING, MISSING, MISSING, MISSING, MISSING);
  private static final float SPACE_WORLD_SCALE = 1.0f;
  private static final double APPROACH_SIZE_MARGIN = 300.0;
  private static final long TICKS_PER_DAY = 24000L;
  public static final double G0 = 0.132;

  private static final Map<OrbitDefinition, OrbitalElements> ELEMENTS = new ConcurrentHashMap<>();

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
      Codec.FLOAT.optionalFieldOf("gravity", 1.0f).forGetter(PlanetDefinition::gravity),
      Codec.FLOAT.optionalFieldOf("surface_latitude", 0.0f).forGetter(PlanetDefinition::surfaceLatitude),
      Codec.FLOAT.optionalFieldOf("surface_longitude", 0.0f).forGetter(PlanetDefinition::surfaceLongitude)
  ).apply(i, (id, size, rot, tilt, orbit, proc, atmo, clouds, rings, dim, grav, lat, lon) -> new PlanetDefinition(id, size, rot, tilt, orbit, proc, atmo, clouds, rings, List.of(), dim, grav, lat, lon)));

  private static final Codec<List<PlanetDefinition>> INLINE_MOONS = Codec.lazyInitialized(() -> PlanetDefinition.CODEC).listOf();

  public static final Codec<PlanetDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
      CORE_CODEC.forGetter(p -> p),
      INLINE_MOONS.optionalFieldOf("moons", List.of()).forGetter(PlanetDefinition::moons)
  ).apply(i, PlanetDefinition::withMoons));

  public PlanetDefinition withMoons(List<PlanetDefinition> newMoons)
  {
    return newMoons == moons ? this : new PlanetDefinition(id, size, axialRotationSpeed, axialTilt, orbit, procedural, atmosphere, clouds, rings, newMoons, dimension, gravity, surfaceLatitude, surfaceLongitude);
  }

  public PlanetDefinition withId(ResourceLocation newId)
  {
    return newId.equals(id) ? this : new PlanetDefinition(newId, size, axialRotationSpeed, axialTilt, orbit, procedural, atmosphere, clouds, rings, moons, dimension, gravity, surfaceLatitude, surfaceLongitude);
  }

  public CubemapTextures resolveTexture()
  {
    return procedural.map(cfg -> PlanetTextureManager.getOrGenerate(cfg, id.toString())).orElse(MISSING_CUBEMAP);
  }

  public OrbitalElements elements()
  {
    return ELEMENTS.computeIfAbsent(orbit, OrbitalElements::fromOrbitDefinition);
  }

  public double mu()
  {
    double r = size * SPACE_WORLD_SCALE;
    return gravity * G0 * r * r;
  }

  public double soiRadius()
  {
    return size * SPACE_WORLD_SCALE * 60.0;
  }

  public double[] currentWorldPosition(long absoluteTick)
  {
    return currentWorldPosition(absoluteTick, 0.0);
  }

  public double[] currentWorldPosition(long absoluteTick, double partialTick)
  {
    Vector3d p = elements().positionAt(absoluteTick, partialTick, new Vector3d());
    if (SPACE_WORLD_SCALE != 1.0f) p.mul(SPACE_WORLD_SCALE);
    return new double[]{ p.x, p.y, p.z };
  }

  public double[] currentWorldVelocity(long absoluteTick, double partialTick)
  {
    Vector3d v = elements().velocityAt(absoluteTick, partialTick, new Vector3d());
    if (SPACE_WORLD_SCALE != 1.0f) v.mul(SPACE_WORLD_SCALE);
    return new double[]{ v.x, v.y, v.z };
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