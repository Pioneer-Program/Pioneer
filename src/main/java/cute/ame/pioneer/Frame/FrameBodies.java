package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.Core.Frame.FrameQuat;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FrameBodies
{
    public static final String SUN = "sun";

    private static volatile Cache cache;

    @Nullable
    public static Body of(SolarSystemDefinition system, PlanetDefinition planet)
    {
        for (Body body : bodies(system))
            if (body.planet() == planet) return body;

        for (Body body : bodies(system))
            if (body.planet() != null && body.planet().id().equals(planet.id())) return body;

        return null;
    }

    @Nullable
    public static SolarSystemDefinition systemOf(ResourceKey<Level> dimension)
    {
        PioneerAPI.DimensionBinding binding = PioneerAPI.getBindingForDimension(dimension).orElse(null);
        if (binding == null) return null;

        return PioneerAPI.getSolarSystem(binding.systemId()).orElse(null);
    }

    public static List<Body> bodies(SolarSystemDefinition system)
    {
        Cache c = cache;
        if (c != null && c.system() == system) return c.bodies();

        List<Body> out = new ArrayList<>(1 + system.planets().size() * 2);
        out.add(new Body(SUN, null, null, system.sun().size()));
        for (PlanetDefinition planet : system.planets())
        {
            out.add(new Body(planet.id().getPath(), planet, null, planet.size()));
            for (PlanetDefinition moon : planet.moons())
                out.add(new Body(moon.id().getPath(), moon, planet, moon.size()));
        }

        List<Body> frozen = List.copyOf(out);
        cache = new Cache(system, frozen);
        return frozen;
    }

    @Nullable
    public static Body find(SolarSystemDefinition system, String name)
    {
        String key = name.toLowerCase(Locale.ROOT);
        for (Body body : bodies(system))
        {
            if (body.name().equals(key)) return body;
            if (body.planet() != null && body.planet().id().toString().equals(key)) return body;
        }
        return null;
    }

    @Nullable
    public static Body nearest(SolarSystemDefinition system, double x, double y, double z, long tick, double partial, double[] distance)
    {
        double[] p = new double[3];
        Body best = null;
        double bestD = Double.POSITIVE_INFINITY;

        for (Body body : bodies(system))
        {
            body.positionAt(tick, partial, p);
            double dx = x - p[0], dy = y - p[1], dz = z - p[2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < bestD)
            {
                bestD = d;
                best = body;
            }
        }

        distance[0] = bestD;
        return best;
    }

    public static String nearestText(@Nullable SolarSystemDefinition system, double x, double y, double z, long tick, double partial)
    {
        if (system == null) return "nearest, no solar system :cc";

        double[] d = new double[1];
        Body body = nearest(system, x, y, z, tick, partial, d);
        if (body == null) return "nearest, no body :/";

        return String.format(Locale.ROOT, "nearest %s d=%.1f (%.2f R)", body.name(), d[0], d[0] / Math.max(body.radius(), 1.0e-6));
    }

    private record Cache(SolarSystemDefinition system, List<Body> bodies)
    {
    }

    public record Body(String name, @Nullable PlanetDefinition planet, @Nullable PlanetDefinition parent,
                       double radius) implements FrameParent
    {
        @Override
        public double[] positionAt(long tick, double partial, double[] out)
        {
            if (planet == null)
            {
                out[0] = out[1] = out[2] = 0.0;
                return out;
            }

            double[] p = planet.currentWorldPosition(tick, partial);
            out[0] = p[0];
            out[1] = p[1];
            out[2] = p[2];
            if (parent != null)
            {
                double[] q = parent.currentWorldPosition(tick, partial);
                out[0] += q[0];
                out[1] += q[1];
                out[2] += q[2];
            }

            return out;
        }

        @Override
        public double[] velocityAt(long tick, double partial, double[] out)
        {
            if (planet == null)
            {
                out[0] = out[1] = out[2] = 0.0;
                return out;
            }

            double[] v = planet.currentWorldVelocity(tick, partial);
            out[0] = v[0];
            out[1] = v[1];
            out[2] = v[2];
            if (parent != null)
            {
                double[] w = parent.currentWorldVelocity(tick, partial);
                out[0] += w[0];
                out[1] += w[1];
                out[2] += w[2];
            }
            return out;
        }

        @Override
        public double[] orientationAt(long tick, double partial, double[] out)
        {
            if (planet == null) return FrameQuat.identity(out);

            double tilt = Math.toRadians(planet.axialTilt()) * 0.5, phase = CelestialMath.axialPhase(planet.axialRotationSpeed(), tick, partial) * 0.5;
            double sz = Math.sin(tilt), cz = Math.cos(tilt), sy = Math.sin(phase), cy = Math.cos(phase);
            out[0] = -sz * sy;
            out[1] = cz * sy;
            out[2] = sz * cy;
            out[3] = cz * cy;
            return out;
        }

        @Override
        public double[] spinAt(long tick, double[] out)
        {
            out[0] = out[1] = out[2] = 0.0;
            if (planet == null) return out;

            double tilt = Math.toRadians(planet.axialTilt()), rate = CelestialMath.axialRate(planet.axialRotationSpeed());
            out[0] = -Math.sin(tilt) * rate;
            out[1] = Math.cos(tilt) * rate;
            return out;
        }

        public double parkDistance()
        {
            return Math.max(3.0 * radius, radius + 500.0);
        }

        public double[] parkingPoint(double fromX, double fromY, double fromZ, long tick, double[] out)
        {
            positionAt(tick, 0.0, out);
            double dx = fromX - out[0], dy = fromY - out[1], dz = fromZ - out[2];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-9)
            {
                dx = -out[0];
                dy = -out[1];
                dz = -out[2];
                len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }

            if (len < 1.0e-9)
            {
                dx = 1.0;
                dy = dz = 0.0;
                len = 1.0;
            }

            double k = parkDistance() / len;
            out[0] += dx * k;
            out[1] += dy * k;
            out[2] += dz * k;
            return out;
        }
    }
}
