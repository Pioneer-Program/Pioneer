package cute.ame.pioneer.SkyPlanet.Physics;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Observer.CubeSurface;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

public final class WorldClock
{
    public record Sample(PlanetDefinition body, int face, long tick, double sunUp, double sunEast, double sunNorth, double hourAngle, double dayFraction)
    {
        public double vanillaSunUp()
        {
            return Math.cos(2.0 * Math.PI * dayFraction);
        }

        public double error()
        {
            return sunUp - vanillaSunUp();
        }
    }

    public static Optional<Double> surfaceDayFraction(Level level, double partialTick)
    {
        return sample(level, partialTick).map(Sample::dayFraction);
    }

    public static Optional<Sample> sample(Level level, double partialTick)
    {
        return surfaceHost(level).map(body -> sample(body, body.homeFace(), level.getGameTime(), partialTick));
    }

    public static Sample sample(PlanetDefinition body, int face, long tick, double partialTick)
    {
        double[] p = body.currentWorldPosition(tick, partialTick);
        double len = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
        Vector3f sun = len > 1.0e-9 ? new Vector3f((float) (-p[0] / len), (float) (-p[1] / len), (float) (-p[2] / len)) : new Vector3f(0f, 0f, 1f);

        double[] w = CubeSurface.normal(face);
        double[] n = new double[3];
        CubeSurface.north(face, n);

        Quaternionf toSystem = CelestialMath.planetOrientation(body.axialTilt(), body.axialRotationSpeed(), tick, (float) partialTick);

        Vector3f up = toSystem.transform(new Vector3f((float) w[0], (float) w[1], (float) w[2])).normalize();
        Vector3f north = toSystem.transform(new Vector3f((float) n[0], (float) n[1], (float) n[2])).normalize();
        Vector3f east = new Vector3f(north).cross(up).normalize();

        double sunUp = sun.dot(up);
        double sunEast = sun.dot(east);
        double sunNorth = sun.dot(north);
        double hourAngle = Math.atan2(sunEast, sunUp);

        double base = Math.acos(Math.clamp(sunUp, -1.0, 1.0)) / (2.0 * Math.PI);
        double fraction = sunEast >= 0.0 ? 1.0 - base : base;
        fraction -= Math.floor(fraction);

        return new Sample(body, face, tick, sunUp, sunEast, sunNorth, hourAngle, fraction);
    }

    public static Optional<PlanetDefinition> surfaceHost(Level level)
    {
        Optional<PioneerAPI.DimensionBinding> bOpt = PioneerAPI.getBindingForDimension(level.dimension());
        if (bOpt.isEmpty()) return Optional.empty();

        PioneerAPI.DimensionBinding binding = bOpt.get();
        if (binding.type() != PioneerAPI.BindingType.SURFACE || binding.planetId() == null) return Optional.empty();

        Optional<SolarSystemDefinition> sOpt = PioneerAPI.getSolarSystem(binding.systemId());
        return sOpt.flatMap(s -> s.findById(binding.planetId()));
    }
}