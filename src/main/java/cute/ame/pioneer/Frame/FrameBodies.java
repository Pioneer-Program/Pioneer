package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Core.Frame.FrameParent;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FrameBodies
{
    public static final String SUN = "sun";

    private static final ThreadLocal<Vector3d> SCRATCH = ThreadLocal.withInitial(Vector3d::new);
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
        int colon = key.indexOf(':');
        for (Body body : bodies(system))
            if (colon < 0 ? body.name().equals(key) : body.planet() != null && isId(body.planet().id(), key, colon))
                return body;

        return null;
    }

    private static boolean isId(ResourceLocation id, String key, int colon)
    {
        String namespace = id.getNamespace(), path = id.getPath();
        return colon == namespace.length() && key.length() == colon + 1 + path.length() && key.startsWith(namespace) && key.startsWith(path, colon + 1);
    }

    @Nullable
    public static Body nearest(SolarSystemDefinition system, double x, double y, double z, long tick, double partial, double[] distance)
    {
        Vector3d p = new Vector3d();
        Body best = null;
        double bestD = Double.POSITIVE_INFINITY;

        for (Body body : bodies(system))
        {
            body.positionAt(tick, partial, p);
            double dx = x - p.x, dy = y - p.y, dz = z - p.z;
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
        public Vector3d positionAt(long tick, double partial, Vector3d dest)
        {
            if (planet == null) return dest.zero();

            Vector3d p = planet.elements().positionAt(tick, partial, SCRATCH.get());
            dest.set(p.x, p.y, p.z);
            if (parent == null) return dest;

            parent.elements().positionAt(tick, partial, p);
            dest.x += p.x;
            dest.y += p.y;
            dest.z += p.z;
            return dest;
        }

        @Override
        public Vector3d velocityAt(long tick, double partial, Vector3d dest)
        {
            if (planet == null) return dest.zero();

            Vector3d v = planet.elements().velocityAt(tick, partial, SCRATCH.get());
            dest.set(v.x, v.y, v.z);
            if (parent == null) return dest;

            parent.elements().velocityAt(tick, partial, v);
            dest.x += v.x;
            dest.y += v.y;
            dest.z += v.z;
            return dest;
        }

        @Override
        public Quaterniond orientationAt(long tick, double partial, Quaterniond dest)
        {
            if (planet == null) return dest.identity();

            double tilt = Math.toRadians(planet.axialTilt()) * 0.5, phase = CelestialMath.axialPhase(planet.axialRotationSpeed(), tick, partial) * 0.5;
            double sz = Math.sin(tilt), cz = Math.cos(tilt), sy = Math.sin(phase), cy = Math.cos(phase);
            return dest.set(-sz * sy, cz * sy, sz * cy, cz * cy);
        }

        @Override
        public Vector3d spinAt(long tick, Vector3d dest)
        {
            if (planet == null) return dest.zero();

            double tilt = Math.toRadians(planet.axialTilt()), rate = CelestialMath.axialRate(planet.axialRotationSpeed());
            return dest.set(-Math.sin(tilt) * rate, Math.cos(tilt) * rate, 0.0);
        }

        public double parkDistance()
        {
            return Math.max(3.0 * radius, radius + 500.0);
        }

        public Vector3d parkingPoint(double fromX, double fromY, double fromZ, long tick, Vector3d dest)
        {
            positionAt(tick, 0.0, dest);
            double dx = fromX - dest.x, dy = fromY - dest.y, dz = fromZ - dest.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-9)
            {
                dx = -dest.x;
                dy = -dest.y;
                dz = -dest.z;
                len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }

            if (len < 1.0e-9)
            {
                dx = 1.0;
                dy = dz = 0.0;
                len = 1.0;
            }

            double k = parkDistance() / len;
            dest.x += dx * k;
            dest.y += dy * k;
            dest.z += dz * k;
            return dest;
        }
    }
}
