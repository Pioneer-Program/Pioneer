package cute.ame.pioneer.SkyPlanet.Rendering.gl;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Core.Render.Debug.GPUProfiler;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;
import cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath;
import cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector;
import cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector.Projected;
import cute.ame.pioneer.SkyPlanet.Star.BuiltinStarTypes;
import cute.ame.pioneer.SkyPlanet.Star.StarTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import static cute.ame.pioneer.SkyPlanet.Rendering.CelestialMath.poseAxialRotation;
public final class SunRenderer
{
    public static void renderRealScale(PoseStack ps, SunDefinition sun, long tick, float partialTick, double dx, double dy, double dz, double dist, Vec3 realCamPos)
    {
        Projected proj = ShellProjector.projectToSafeShell(dx, dy, dz, dist, (float) Math.max(sun.size(), ShellProjector.MIN_APPARENT));
        ps.pushPose();
        ps.translate(proj.dx, proj.dy, proj.dz);
        boolean rendersAsPostProcess = sun.type().equals(BuiltinStarTypes.BLACK_HOLE);

        if (!rendersAsPostProcess)
        {
            float camDist = (float) Math.sqrt(proj.dx * proj.dx + proj.dy * proj.dy + proj.dz * proj.dz);
            float cx = (float) (-dx / dist), cy = (float) (-dy / dist), cz = (float) (-dz / dist);
            float spin = CelestialMath.axialPhaseRadians(sun.axialRotationSpeed(), tick, partialTick);
            float timeSeconds = (tick + partialTick) / 20.0f;

            GPUProfiler.begin("celestial.star.core");
            StarRenderer.render(ps, sun, cx * camDist, cy * camDist, cz * camDist, proj.size, spin, timeSeconds);
            GPUProfiler.end();
        }
        poseAxialRotation(ps, sun.axialRotationSpeed(), tick, partialTick);
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        StarTypeRegistry.get(sun.type()).renderExtras(ps, sun, tick, partialTick, proj.size * sun.size(), bufferSource, dx, dy, dz, realCamPos);
        ps.popPose();
    }
}
