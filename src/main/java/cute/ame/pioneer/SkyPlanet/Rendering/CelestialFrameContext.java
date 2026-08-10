package cute.ame.pioneer.SkyPlanet.Rendering;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record CelestialFrameContext(
    Vec3 effectiveCamPos,
    ResourceLocation selfPlanetId,
    float selfPlanetAlpha,
    ResourceLocation excludedPlanetId,
    float selfClimbOffset,
    float selfAscensionProgress,
    long tick,
    boolean useAngularFloor,
    boolean selfIsSurfaceType,
    float selfTiltProgress
)
{
    public boolean isSelf(ResourceLocation planetId)
    {
        return selfPlanetId != null && planetId.equals(selfPlanetId);
    }

    public boolean isExcluded(ResourceLocation planetId)
    {
        return excludedPlanetId != null && planetId.equals(excludedPlanetId);
    }
}