package cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI;
import cute.ame.auralithpioneerinitiative.Config;
import cute.ame.auralithpioneerinitiative.Core.Render.Debug.GPUProfiler;
import cute.ame.auralithpioneerinitiative.Seamless.Client.SeamlessGhostSurfacePatchRenderer;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.PlanetDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector.Projected;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector.MIN_APPARENT;
import static cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.ShellProjector.projectToSafeShell;

public final class SolarSystemRenderer
{
    private static final SolarSystemRenderer INSTANCE = new SolarSystemRenderer();
    public static SolarSystemRenderer getInstance() { return INSTANCE; }
    public static final float ORBIT_PLANET_SIZE = 60.0f;

    private record RenderJob(double distance, Runnable draw) {}

    public void renderSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, ClientLevel level)
    {
        skyFogSetup.run();
        Matrix4f rotOnly = new Matrix4f(frustumMatrix);
        rotOnly.m03(0.0f).m13(0.0f).m23(0.0f).m30(0.0f).m31(0.0f).m32(0.0f);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        PoseStack ps = new PoseStack();
        ps.mulPose(rotOnly);
        renderCelestials(ps, camera, level, partialTick, projectionMatrix);

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        GPUProfiler.endFrame();
    }

    private void renderCelestials(PoseStack ps, Camera camera, ClientLevel level, float partialTick, Matrix4f projMat)
    {
        Optional<AuralithAPI.DimensionBinding> bOpt = AuralithAPI.getBindingForDimension(level.dimension());
        if (bOpt.isEmpty()) return;
        AuralithAPI.DimensionBinding binding = bOpt.get();

        Optional<SolarSystemDefinition> sOpt = AuralithAPI.getSolarSystem(binding.systemId());
        if (sOpt.isEmpty()) return;
        SolarSystemDefinition system = sOpt.get();

        Vec3 effectiveCamPos = computeEffectiveCamPos(binding, system, camera, level);
        boolean isPlanetLocked = binding.type() == AuralithAPI.BindingType.SURFACE;

        ResourceLocation selfPlanetId = null;
        float selfPlanetAlpha = 1.0f;

        ResourceLocation excludedPlanetId = null;
        float selfClimbOffset = 0.0f;
        float selfAscensionProgress = 0.0f;
        float selfTiltProgress = 1.0f;
        if (isPlanetLocked && binding.planetId() != null)
        {
            int startY = Config.SHOW_OWN_PLANET_START_Y.get();
            int endY   = Config.ORBIT_ENTRY_Y.get();
            float altitude = (float) camera.getPosition().y;
            selfClimbOffset = altitude - startY;
            selfAscensionProgress = Mth.clamp(selfClimbOffset / (endY - startY), 0.0f, 1.0f);

            int tiltStartY = Config.SELF_TILT_START_Y.get();
            int tiltEndY = endY - Config.SELF_TILT_END_OFFSET.get();
            selfTiltProgress = (tiltEndY > tiltStartY) ? Mth.clamp((altitude - tiltStartY) / (tiltEndY - tiltStartY), 0.0f, 1.0f) : 1.0f;

            float alpha = Mth.clamp(selfAscensionProgress * 2.0f, 0.0f, 1.0f);
            if (alpha > 0.0f)
            {
                selfPlanetId   = binding.planetId();
                selfPlanetAlpha = alpha;
            }
            else
            {
                excludedPlanetId = binding.planetId();
            }
        }

        long tick = level.getGameTime();
        ps.pushPose();

        CelestialFrameContext ctx = new CelestialFrameContext(
                effectiveCamPos, selfPlanetId, selfPlanetAlpha, excludedPlanetId,
                selfClimbOffset, selfAscensionProgress, tick, !isPlanetLocked,
                binding.type() == AuralithAPI.BindingType.SURFACE, selfTiltProgress);
        renderSystemUnified(ps, system, ctx, partialTick, camera.getPosition(), projMat);
        ps.popPose();
    }

    private Vec3 computeEffectiveCamPos(AuralithAPI.DimensionBinding binding, SolarSystemDefinition system, Camera camera, ClientLevel level)
    {

        if ((binding.type() == AuralithAPI.BindingType.SURFACE) && binding.planetId() != null)
        {
            Optional<PlanetDefinition> planetOpt = system.findById(binding.planetId());
            if (planetOpt.isPresent())
            {
                double[] planetPos = planetOpt.get().currentWorldPosition(level.getGameTime());

                return new Vec3(planetPos[0], planetPos[1], planetPos[2]);
            }
        }

        return camera.getPosition();
    }

    private void renderSystemUnified(PoseStack ps, SolarSystemDefinition system, CelestialFrameContext ctx, float partialTick, Vec3 realCamPos, Matrix4f projMat)
    {
        Vec3 effectiveCamPos = ctx.effectiveCamPos();
        long tick = ctx.tick();

        GPUProfiler.begin("skybox.galaxy");
        GalaxyRenderer.render(ps, projMat, tick, partialTick);
        GPUProfiler.end();

        {
            double sdx = -effectiveCamPos.x, sdy = -effectiveCamPos.y, sdz = -effectiveCamPos.z;
            double sdist = Math.sqrt(sdx * sdx + sdy * sdy + sdz * sdz);
            if (sdist > 1e-6)
            {
                SunRenderer.renderRealScale(ps, system.sun(), tick, partialTick, sdx, sdy, sdz, sdist, realCamPos);
            }
        }

        List<RenderJob> jobs = new ArrayList<>();

        for (PlanetDefinition planet : system.planets())
        {
            if (ctx.isExcluded(planet.id())) continue;

            boolean isSelf = ctx.isSelf(planet.id());
            float alpha = isSelf ? ctx.selfPlanetAlpha() : 1.0f;

            double angle = planet.orbit().computeAngle(tick, partialTick);
            double radius = planet.orbit().computeCurrentRadius(angle);
            float[] pos = planet.orbit().compute3DPosition(angle, radius, 1.0f);

            double dx, dy, dz, dist;
            if (isSelf)
            {

                dx = 0.0;

                dy = (-(double) planet.size() * 0.5f) + -ctx.selfClimbOffset();
                dz = 0.0;
                dist = Math.max(Math.abs(dy), MIN_APPARENT);
            }
            else
            {
                dx = pos[0] - effectiveCamPos.x;
                dy = pos[1] - effectiveCamPos.y;
                dz = pos[2] - effectiveCamPos.z;
                dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 1e-6) continue;
            }

            final double fdx = dx, fdy = dy, fdz = dz, fdist = dist;
            final float[] fpos = pos;

            jobs.add(new RenderJob(dist, () ->
                    renderPlanetBody(ps, planet, isSelf, alpha, fpos, fdx, fdy, fdz, fdist, ctx, tick, partialTick)));

            for (PlanetDefinition moon : planet.moons())
            {
                double mAngle = moon.orbit().computeAngle(tick, partialTick);
                double mRadius = moon.orbit().computeCurrentRadius(mAngle);
                float[] mLocalPos = moon.orbit().compute3DPosition(mAngle, mRadius, 1.0f);
                double mx = fpos[0] + mLocalPos[0];
                double my = fpos[1] + mLocalPos[1];
                double mz = fpos[2] + mLocalPos[2];
                double mdx = mx - effectiveCamPos.x, mdy = my - effectiveCamPos.y, mdz = mz - effectiveCamPos.z;
                double mdist = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
                if (mdist < 1e-6) continue;

                jobs.add(new RenderJob(mdist, () -> MoonRenderer.renderRealScale(ps, moon, fpos, effectiveCamPos, tick, partialTick)));
            }
        }

        jobs.sort((a, b) -> Double.compare(b.distance(), a.distance()));
        for (RenderJob job : jobs) job.draw().run();
    }

    private void renderPlanetBody(PoseStack ps, PlanetDefinition planet, boolean isSelf, float alpha, float[] pos, double dx, double dy, double dz, double dist, CelestialFrameContext ctx, long tick, float partialTick)
    {
        float cdx = (float) (dx / dist), cdy = (float) (dy / dist), cdz = (float) (dz / dist);
        float realSize = Math.max(planet.size(), MIN_APPARENT);

        Projected proj = projectToSafeShell(dx, dy, dz, dist, realSize);
        float apparentSize = proj.size;
        float effectiveTiltDegrees = isSelf ? CelestialMath.lerp(0.0f, planet.axialTilt(), ctx.selfTiltProgress()) : planet.axialTilt();
        Quaternionf orientation = new Quaternionf().rotationZ((float) Math.toRadians(effectiveTiltDegrees)).rotateY(CelestialMath.axialPhaseRadians(planet.axialRotationSpeed(), tick, partialTick));

        ps.pushPose();
        ps.translate(proj.dx, proj.dy, proj.dz);
        ps.mulPose(orientation);

        float psx = -pos[0], psy = -pos[1], psz = -pos[2];
        float plen = (float) Math.sqrt(psx * psx + psy * psy + psz * psz);
        Vector3f worldSun = (plen > 1e-6f) ? new Vector3f(psx / plen, psy / plen, psz / plen) : new Vector3f(0f, 0f, 1f);

        Quaternionf invRot = orientation.conjugate(new Quaternionf());

        Vector3f localSun = invRot.transform(new Vector3f(worldSun));
        Vector3f localCam = invRot.transform(new Vector3f(cdx, cdy, cdz));
        float sunLX = localSun.x, sunLY = localSun.y, sunLZ = localSun.z;
        float camLX = localCam.x, camLY = localCam.y, camLZ = localCam.z;

        planet.atmosphere().ifPresent(atmo ->
        {
            ps.pushPose();
            ps.scale(apparentSize, apparentSize, apparentSize);

            float camDistObj = (float) Math.sqrt(proj.dx * proj.dx + proj.dy * proj.dy + proj.dz * proj.dz) / apparentSize;

            GPUProfiler.begin("planets.volumetrics.atmosphere");
            AtmosphereRenderer.render(ps, atmo, -camLX, -camLY, -camLZ, sunLX, sunLY, sunLZ, camDistObj);
            GPUProfiler.end();

            ps.popPose();
        });

        planet.rings().ifPresent(rings -> RingMeshRenderer.render(ps, rings, apparentSize, sunLX, sunLY, sunLZ));
        float ringInnerR = planet.rings().map(r -> r.innerRadius() * 0.5f).orElse(CubeMeshRenderer.NO_RINGS);
        float ringOuterR = planet.rings().map(r -> r.outerRadius() * 0.5f).orElse(CubeMeshRenderer.NO_RINGS);

        ps.pushPose();
        ps.scale(apparentSize, apparentSize, apparentSize);

        CubeMeshRenderer.renderTextureCubeShaded(ps, planet.resolveTexture(), true, sunLX, sunLY, sunLZ, alpha, ringInnerR, ringOuterR);

        planet.clouds().ifPresent(clouds ->
        {
            ps.pushPose();

            float camDistObj = (float) Math.sqrt(proj.dx * proj.dx + proj.dy * proj.dy + proj.dz * proj.dz) / apparentSize;
            float timeSeconds = (tick + partialTick) / 20.0f;

            GPUProfiler.begin("planets.volumetrics.clouds");
            CloudsRenderer.render(ps, clouds, -camLX, -camLY, -camLZ, sunLX, sunLY, sunLZ, camDistObj, timeSeconds);
            GPUProfiler.end();

            ps.popPose();
        });

        SeamlessGhostSurfacePatchRenderer.renderPatch(ps, planet, -camLX, -camLY, -camLZ);
        ps.popPose();
        ps.popPose();
    }
}