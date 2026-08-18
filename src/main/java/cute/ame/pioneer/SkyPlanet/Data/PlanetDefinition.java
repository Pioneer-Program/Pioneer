package cute.ame.pioneer.SkyPlanet.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cute.ame.pioneer.Core.Observer.CubeSurface;
import cute.ame.pioneer.Core.Render.Cache.PlanetTextureManager;
import cute.ame.pioneer.Core.Render.Helper.CubemapTextures;
import cute.ame.pioneer.SkyPlanet.Orbit.OrbitalElements;
import cute.ame.pioneer.SkyPlanet.Physics.PlanetaryPhysics;
import cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public record PlanetDefinition
(
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
  float massEarth,
  float bondAlbedo,
  SurfaceMapping surface
)
{
  public static final ResourceLocation MISSING = ResourceLocation.withDefaultNamespace("missingno");
  private static final CubemapTextures MISSING_CUBEMAP = new CubemapTextures(MISSING, MISSING, MISSING, MISSING, MISSING, MISSING);
  private static final float SPACE_WORLD_SCALE = 1.0f;
  private static final double APPROACH_SIZE_MARGIN = 300.0;
  public static final double G0 = 0.132;

  private static final double MIN_HALF_SIDE_BLOCKS = 64.0;
  private static final double MAX_HALF_SIDE_BLOCKS = 2.9e7;

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
    Codec.FLOAT.optionalFieldOf("mass_earth", 1.0f).forGetter(PlanetDefinition::massEarth),
    Codec.FLOAT.optionalFieldOf("bond_albedo", 0.3f).forGetter(PlanetDefinition::bondAlbedo),
    SurfaceMapping.MAP_CODEC.forGetter(PlanetDefinition::surface)
  ).apply(i, (id, size, rot, tilt, orbit, proc, atmo, clouds, rings, dim, mass, albedo, surf) -> new PlanetDefinition(id, size, rot, tilt, orbit, proc, atmo, clouds, rings, List.of(), dim, mass, albedo, surf)));

  private static final Codec<List<PlanetDefinition>> INLINE_MOONS = Codec.lazyInitialized(() -> PlanetDefinition.CODEC).listOf();

  public static final Codec<PlanetDefinition> CODEC = RecordCodecBuilder.<PlanetDefinition>create(i -> i.group(CORE_CODEC.forGetter(p -> p), INLINE_MOONS.optionalFieldOf("moons", List.of()).forGetter(PlanetDefinition::moons)
  ).apply(i, PlanetDefinition::withMoons)).flatXmap(PlanetDefinition::validate, DataResult::success);

  private static DataResult<PlanetDefinition> validate(PlanetDefinition p)
  {
    if (!(p.size() > 0f))
      return DataResult.error(() -> p.id() + " : size must be > 0");
    if (!(p.massEarth() > 0f))
      return DataResult.error(() -> p.id() + " : mass_earth must be > 0");

    if (p.dimension().isEmpty()) return DataResult.success(p);

    if (!(p.surfaceScale() > 0f))
      return DataResult.error(() -> p.id() + " : surface_scale must be > 0");
    if (!(p.verticalScale() > 0f))
      return DataResult.error(() -> p.id() + " : vertical_scale must be > 0");

    double half = CubeSurface.halfSide(p.size() * 0.5, p.surfaceScale());
    if (half > MAX_HALF_SIDE_BLOCKS)
      return DataResult.error(() -> p.id() + " : surface dimension " + p.dimension().get() + ", half-side = " + (long) half + " blocks, beyond the world border. Increase surface_scale.");
    if (half < MIN_HALF_SIDE_BLOCKS)
      return DataResult.error(() -> p.id() + " : surface dimension " + p.dimension().get() + ", half-side = " + (long) half + " blocks, too small for a playable world. Decrease surface_scale.");

    return DataResult.success(p);
  }

  public PlanetDefinition withMoons(List<PlanetDefinition> newMoons)
  {
    return newMoons == moons ? this : new PlanetDefinition(id, size, axialRotationSpeed, axialTilt, orbit, procedural, atmosphere, clouds, rings, newMoons, dimension, massEarth, bondAlbedo, surface);
  }

  public PlanetDefinition withId(ResourceLocation newId)
  {
    return newId.equals(id) ? this : new PlanetDefinition(newId, size, axialRotationSpeed, axialTilt, orbit, procedural, atmosphere, clouds, rings, moons, dimension, massEarth, bondAlbedo, surface);
  }

  public CubemapTextures resolveTexture()
  {
    return procedural.map(cfg -> PlanetTextureManager.getOrGenerate(cfg, id.toString())).orElse(MISSING_CUBEMAP);
  }

  public OrbitalElements elements()
  {
    return ELEMENTS.computeIfAbsent(orbit, OrbitalElements::fromOrbitDefinition);
  }

  public int homeFace()
  {
    return surface.homeFace();
  }

  public float homeU()
  {
    return surface.homeU();
  }

  public float homeV()
  {
    return surface.homeV();
  }

  public float surfaceScale()
  {
    return surface.surfaceScale();
  }

  public float verticalScale()
  {
    return surface.verticalScale();
  }

  public float gravity()
  {
    return (float) PlanetaryPhysics.surfaceGravityRelative(massEarth, size);
  }

  public double escapeVelocityKmS()
  {
    return PlanetaryPhysics.escapeVelocityKmS(massEarth, size);
  }

  public double mu()
  {
    double r = size * SPACE_WORLD_SCALE;
    return gravity() * G0 * r * r;
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
    return new double[]{ p.x, p.y, p.z };
  }

  public double[] currentWorldVelocity(long absoluteTick, double partialTick)
  {
    Vector3d v = elements().velocityAt(absoluteTick, partialTick, new Vector3d());
    return new double[]{ v.x, v.y, v.z };
  }

  public double approachRadius()
  {
    return size + APPROACH_SIZE_MARGIN;
  }

  public Quaternionf computeTrueRotation(long tick, float partialTick)
  {
    double dayTicks = axialRotationSpeed * (double) CelestialMath.TICKS_PER_DAY;
    float spinAngle = 0.0f;
    if (Math.abs(dayTicks) > 1.0e-6)
    {
      double phase = ((tick + partialTick) / dayTicks) % 1.0;
      spinAngle = (float) (phase * 2.0 * Math.PI);
    }
    float tiltRad = (float) Math.toRadians(axialTilt);
    return new Quaternionf().rotationZ(tiltRad).mul(new Quaternionf().rotationY(spinAngle));
  }
}