package cute.ame.pioneer.SkyPlanet.Physics;

import cute.ame.pioneer.SkyPlanet.Data.AtmosphereDefinition;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;

public record PlanetEnvironment
(
    double distanceAu,
    double gravityMs2,
    double irradianceRelative,
    double equilibriumTempK,
    double surfaceTempK
)
{
    public static PlanetEnvironment of(SunDefinition sun, PlanetDefinition planet)
    {
        double au = PhysicalScale.semiMajorAxisAu(planet.orbit(), sun.massSolar());
        double gravity = PlanetaryPhysics.surfaceGravityMs2(planet.massEarth(), planet.radiusKm());

        double lum = StellarPhysics.luminositySolar(sun.massSolar(), sun.stage());
        double irradiance = StellarPhysics.irradianceRelative(lum, au);
        double tEq = PlanetaryPhysics.equilibriumTemperatureK(StellarPhysics.temperatureK(sun.massSolar(), sun.stage()), StellarPhysics.radiusSolar(sun.massSolar(), sun.stage()), au, planet.bondAlbedo());

        double tSurf = tEq;
        if (planet.atmosphere().isPresent())
        {
            AtmosphereDefinition atmo = planet.atmosphere().get();
            tSurf += PlanetaryPhysics.greenhouseLiftK(atmo.surfacePressureBar(), atmo.greenhouseFraction());
        }

        return new PlanetEnvironment(au, gravity, irradiance, tEq, tSurf);
    }

    public static PlanetEnvironment ofMoon(SunDefinition sun, PlanetDefinition parent, PlanetDefinition moon)
    {
        double au = PhysicalScale.semiMajorAxisAu(parent.orbit(), sun.massSolar());
        double gravity = PlanetaryPhysics.surfaceGravityMs2(moon.massEarth(), moon.radiusKm());
        double lum = StellarPhysics.luminositySolar(sun.massSolar(), sun.stage());
        double irradiance = StellarPhysics.irradianceRelative(lum, au);
        double tEq = PlanetaryPhysics.equilibriumTemperatureK(StellarPhysics.temperatureK(sun.massSolar(), sun.stage()), StellarPhysics.radiusSolar(sun.massSolar(), sun.stage()), au, moon.bondAlbedo());

        double tSurf = tEq;
        if (moon.atmosphere().isPresent())
        {
            AtmosphereDefinition atmo = moon.atmosphere().get();
            tSurf += PlanetaryPhysics.greenhouseLiftK(atmo.surfacePressureBar(), atmo.greenhouseFraction());
        }

        return new PlanetEnvironment(au, gravity, irradiance, tEq, tSurf);
    }

    public double gravityRelative()
    {
        return gravityMs2 / PlanetaryPhysics.EARTH_GRAVITY_MS2;
    }
}