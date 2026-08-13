package cute.ame.pioneer.SkyPlanet.Rendering;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

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
    float selfTiltProgress,
    Quaternionf horizonRotation
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