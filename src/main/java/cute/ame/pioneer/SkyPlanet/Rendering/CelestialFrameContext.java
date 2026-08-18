package cute.ame.pioneer.SkyPlanet.Rendering;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public record CelestialFrameContext
(
    Vec3 effectiveCamPos,
    ResourceLocation selfPlanetId,
    float selfPlanetAlpha,
    ResourceLocation excludedPlanetId,
    Vector3d selfOffsetKm,
    float selfAscensionProgress,
    long tick,
    boolean useAngularFloor,
    boolean selfIsSurfaceType,
    Quaternionf horizonRotation,
    float starVisibility
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