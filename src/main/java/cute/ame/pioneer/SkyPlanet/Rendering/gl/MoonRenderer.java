package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Rendering.PhysicalScale;
import cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector;
import cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector.Projected;
import net.minecraft.world.phys.Vec3;

import static cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath.poseAxialRotation;

public final class MoonRenderer
{
    public static void renderRealScale(PoseStack ps, PlanetDefinition moon, double[] worldPos, Vec3 camPos, long tick, float partialTick)
    {
        double dx = worldPos[0] - camPos.x;
        double dy = worldPos[1] - camPos.y;
        double dz = worldPos[2] - camPos.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-6) return;

        float realSize = Math.max(moon.size(), ShellProjector.MIN_APPARENT * 0.3f);
        Projected proj = ShellProjector.projectToSafeShell(dx, dy, dz, dist, realSize);

        ps.pushPose();
        ps.translate(proj.dx, proj.dy, proj.dz);
        poseAxialRotation(ps, moon.axialRotationSpeed(), tick, partialTick);
        ps.scale(proj.size, proj.size, proj.size);

        float camDistObj = (float) Math.sqrt(proj.dx * proj.dx + proj.dy * proj.dy + proj.dz * proj.dz) / proj.size;
        float cx = (float) (dx / dist), cy = (float) (dy / dist), cz = (float) (dz / dist);

        BodyRenderer.render(ps, moon.resolveTexture(), 1.0f, -cx * camDistObj, -cy * camDistObj, -cz * camDistObj, 0f, 0f, 1f, null, PhysicalScale.SOLAR_ANG_RAD_1AU, false);
        ps.popPose();
    }
}