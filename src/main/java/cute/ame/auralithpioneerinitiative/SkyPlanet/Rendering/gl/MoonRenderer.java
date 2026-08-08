package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.PlanetDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector.Projected;
import net.minecraft.world.phys.Vec3;

import static cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.CelestialMath.poseAxialRotation;

public final class MoonRenderer
{
    public static void renderRealScale(PoseStack ps, PlanetDefinition moon, float[] parentPos, Vec3 camPos, long tick, float partialTick)
    {
        double angle = moon.orbit().computeAngle(tick, partialTick);
        double radius = moon.orbit().computeCurrentRadius(angle);
        float[] localPos = moon.orbit().compute3DPosition(angle, radius, 1.0f);

        double mx = parentPos[0] + localPos[0];
        double my = parentPos[1] + localPos[1];
        double mz = parentPos[2] + localPos[2];

        double dx = mx - camPos.x, dy = my - camPos.y, dz = mz - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-6) return;

        float realSize = Math.max(moon.size(), ShellProjector.MIN_APPARENT * 0.3f);
        Projected proj = ShellProjector.projectToSafeShell(dx, dy, dz, dist, realSize);

        ps.pushPose();
        ps.translate(proj.dx, proj.dy, proj.dz);
        poseAxialRotation(ps, moon.axialRotationSpeed(), tick, partialTick);
        ps.scale(proj.size, proj.size, proj.size);
        CubeMeshRenderer.renderTextureCube(ps, moon.resolveTexture(), true);
        ps.popPose();
    }
}
