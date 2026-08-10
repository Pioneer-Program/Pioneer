package cute.ame.pioneer.Core.API;

import cute.ame.pioneer.SkyPlanet.Data.AtmosphereDefinition;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Supplier;

public final class AuralithAPI
{
  private static final Map<ResourceLocation, SolarSystemDefinition> SYSTEMS = new LinkedHashMap<>();
  private static final Map<ResourceLocation, DimensionBinding> DIM_BINDINGS = new HashMap<>();

  public enum BindingType { SURFACE, SPACE }

  public record DimensionBinding(ResourceLocation systemId, ResourceLocation planetId, BindingType type)
  {
    public boolean isSpaceDimension() { return type == BindingType.SPACE; }
    public boolean isSurfaceDimension() { return type == BindingType.SURFACE; }
  }

  public record PhysicsEntry(double mass, Supplier<Vec3> posSupplier) {}

  public static void registerSolarSystem(ResourceLocation id, SolarSystemDefinition definition)
  {
    SYSTEMS.put(id, definition);
    for (PlanetDefinition planet : definition.planets())
    {
      planet.dimension().ifPresent(dimId -> DIM_BINDINGS.put(dimId, new DimensionBinding(id, planet.id(), BindingType.SURFACE)));

      for (PlanetDefinition moon : planet.moons())
        moon.dimension().ifPresent(dimId -> DIM_BINDINGS.put(dimId, new DimensionBinding(id, moon.id(), BindingType.SURFACE)));
    }
    definition.spaceDimension().ifPresent(spaceDim -> DIM_BINDINGS.put(spaceDim, new DimensionBinding(id, null, BindingType.SPACE)));
  }

  public static void bindDimensionToPlanet(ResourceLocation dimensionId, ResourceLocation systemId, ResourceLocation planetId)
  {
    DIM_BINDINGS.put(dimensionId, new DimensionBinding(systemId, planetId, BindingType.SURFACE));
  }

  public static Optional<DimensionBinding> getBindingForDimension(ResourceKey<Level> dimension)
  {
    return Optional.ofNullable(DIM_BINDINGS.get(dimension.location()));
  }

  public static Optional<SolarSystemDefinition> getSolarSystem(ResourceLocation id)
  {
    return Optional.ofNullable(SYSTEMS.get(id));
  }

  public static Map<ResourceLocation, SolarSystemDefinition> getAllSystems()
  {
    return Collections.unmodifiableMap(SYSTEMS);
  }

  public static boolean hasSkyFor(ResourceKey<Level> dimension)
  {
    return DIM_BINDINGS.containsKey(dimension.location());
  }

  public static boolean isSpaceDimension(ResourceKey<Level> dimension)
  {
    return getBindingForDimension(dimension).map(DimensionBinding::isSpaceDimension).orElse(false);
  }

  public static boolean isSurfaceDimension(ResourceKey<Level> dimension)
  {
    return getBindingForDimension(dimension).map(DimensionBinding::isSurfaceDimension).orElse(false);
  }

  public static float getGravityFor(ResourceKey<Level> dimension)
  {
    DimensionBinding binding = DIM_BINDINGS.get(dimension.location());
    if (binding == null) return 1.0f;
    if (binding.type() != BindingType.SURFACE) return 0.0f;

    SolarSystemDefinition system = SYSTEMS.get(binding.systemId());
    if (system == null) return 1.0f;

    return system.planets().stream().filter(p -> p.id().equals(binding.planetId())).findFirst().map(PlanetDefinition::gravity).orElse(1.0f);
  }

  public static boolean isBreathable(ResourceKey<Level> dimension)
  {
    DimensionBinding binding = DIM_BINDINGS.get(dimension.location());
    if (binding == null) return true;
    if (binding.type() != BindingType.SURFACE) return false;

    SolarSystemDefinition system = SYSTEMS.get(binding.systemId());
    if (system == null) return true;

    return system.planets().stream().filter(p -> p.id().equals(binding.planetId())).findFirst().flatMap(PlanetDefinition::atmosphere).flatMap(AtmosphereDefinition::breathable).orElse(false);
  }

  public static boolean isUnbreathable(ServerPlayer player)
  {
    return getBindingForDimension(player.level().dimension()).map(b -> !b.isSurfaceDimension() || !isBreathable(player.level().dimension())).orElse(true);
  }

  public static void clearAll()
  {
    SYSTEMS.clear();
    DIM_BINDINGS.clear();
  }
}