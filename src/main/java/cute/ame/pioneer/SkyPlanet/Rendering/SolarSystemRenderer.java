package cute.ame.pioneer.SkyPlanet.Rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Render.Debug.GPUProfiler;
import cute.ame.pioneer.Seamless.Client.SeamlessGhostSurfacePatchRenderer;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;
import cute.ame.pioneer.SkyPlanet.Physics.PlanetEnvironment;
import cute.ame.pioneer.SkyPlanet.Physics.SkyBrightness;
import cute.ame.pioneer.SkyPlanet.Physics.SurfaceCoordinates;
import cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector.Projected;
import cute.ame.pioneer.SkyPlanet.Rendering.gl.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector.MIN_APPARENT;
import static cute.ame.pioneer.SkyPlanet.Rendering.ShellProjector.projectToSafeShell;

public final class SolarSystemRenderer
{
    private static final SolarSystemRenderer INSTANCE = new SolarSystemRenderer();

    public static SolarSystemRenderer getInstance()
    {
        return INSTANCE;
    }

    private record RenderJob(double distance, Runnable draw) {}

    private static final float CUBE_CORNER = 0.8660254f;

    private final Matrix4f cullMatrix = new Matrix4f();
    private final Matrix4f cullMatrixOriented = new Matrix4f();
    private final FrustumIntersection frustum = new FrustumIntersection();

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
        Optional<PioneerAPI.DimensionBinding> bOpt = PioneerAPI.getBindingForDimension(level.dimension());
        if (bOpt.isEmpty()) return;
        PioneerAPI.DimensionBinding binding = bOpt.get();

        Optional<SolarSystemDefinition> sOpt = PioneerAPI.getSolarSystem(binding.systemId());
        if (sOpt.isEmpty()) return;
        SolarSystemDefinition system = sOpt.get();

        long tick = level.getDayTime();
        double animSeconds = (level.getGameTime() + (double) partialTick) * PhysicalScale.SECONDS_PER_TICK;

        Vec3 effectiveCamPos = computeEffectiveCamPos(binding, system, camera, tick, partialTick);
        boolean isPlanetLocked = binding.type() == PioneerAPI.BindingType.SURFACE;

        ResourceLocation selfPlanetId = binding.planetId();
        ResourceLocation excludedPlanetId = null;
        float selfClimbOffset = 0.0f;
        float selfAscensionProgress = 0.0f;
        float selfTiltProgress = 1.0f;
        if (isPlanetLocked && binding.planetId() != null)
        {
            int startY = Config.SHOW_OWN_PLANET_START_Y.get();
            int endY = Config.ORBIT_ENTRY_Y.get();
            float altitude = (float) camera.getPosition().y;
            selfClimbOffset = altitude - startY;
            selfAscensionProgress = Mth.clamp(selfClimbOffset / (endY - startY), 0.0f, 1.0f);

            int tiltStartY = Config.SELF_TILT_START_Y.get();
            int tiltEndY = endY - Config.SELF_TILT_END_OFFSET.get();
            selfTiltProgress = (tiltEndY > tiltStartY) ? Mth.clamp((altitude - tiltStartY) / (tiltEndY - tiltStartY), 0.0f, 1.0f) : 1.0f;
        }

        ps.pushPose();

        Quaternionf horizon = null;
        float starVis = 1.0f;
        if (isPlanetLocked && binding.planetId() != null)
        {
            Optional<PlanetDefinition> selfOpt = system.findById(binding.planetId());
            if (selfOpt.isPresent())
            {
                PlanetDefinition self = selfOpt.get();

                Vec3 here = camera.getPosition();
                double latDeg = SurfaceCoordinates.latitudeDeg(self, here.x, here.z);
                double lonDeg = SurfaceCoordinates.longitudeDeg(self, here.x, here.z);

                double[] sp = self.currentWorldPosition(tick, partialTick);
                float sl = (float) Math.sqrt(sp[0] * sp[0] + sp[1] * sp[1] + sp[2] * sp[2]);
                Vector3f worldSun = (sl > 1e-6f) ? new Vector3f((float) -sp[0] / sl, (float) -sp[1] / sl, (float) -sp[2] / sl) : new Vector3f(0f, 0f, 1f);

                horizon = CelestialMath.localHorizonRotation(self.axialTilt(), self.axialRotationSpeed(), latDeg, lonDeg, worldSun, tick, partialTick);
                Vector3f sunLocal = horizon.transform(new Vector3f(worldSun));
                starVis = SkyBrightness.starVisibility(sunLocal.y, self);
            }
        }

        CelestialFrameContext ctx = new CelestialFrameContext(effectiveCamPos, selfPlanetId, 1.0f, excludedPlanetId, selfClimbOffset, selfAscensionProgress, tick, !isPlanetLocked, binding.type() == PioneerAPI.BindingType.SURFACE, selfTiltProgress, horizon, starVis);
        renderSystemUnified(ps, system, ctx, partialTick, camera.getPosition(), projMat, animSeconds);
        ps.popPose();
    }

    private Vec3 computeEffectiveCamPos(PioneerAPI.DimensionBinding binding, SolarSystemDefinition system, Camera camera, long tick, float partialTick)
    {
        if ((binding.type() == PioneerAPI.BindingType.SURFACE) && binding.planetId() != null)
        {
            Optional<PlanetDefinition> planetOpt = system.findById(binding.planetId());
            if (planetOpt.isPresent())
            {
                double[] planetPos = planetOpt.get().currentWorldPosition(tick, partialTick);
                return new Vec3(planetPos[0], planetPos[1], planetPos[2]);
            }
        }

        return camera.getPosition();
    }

    private void renderSystemUnified(PoseStack ps, SolarSystemDefinition system, CelestialFrameContext ctx, float partialTick, Vec3 realCamPos, Matrix4f projMat, double animSeconds)
    {
        Vec3 effectiveCamPos = ctx.effectiveCamPos();
        long tick = ctx.tick();

        final Quaternionf horizon = ctx.horizonRotation();

        cullMatrix.set(projMat).mul(ps.last().pose());
        if (horizon != null) frustum.set(cullMatrixOriented.set(cullMatrix).rotate(horizon));
        else frustum.set(cullMatrix);

        GPUProfiler.begin("skybox.galaxy");
        oriented(ps, horizon, () -> GalaxyRenderer.render(ps, projMat, tick, partialTick, ctx.starVisibility())).run();
        GPUProfiler.end();

        List<RenderJob> jobs = new ArrayList<>();

        {
            final double sdx = -effectiveCamPos.x, sdy = -effectiveCamPos.y, sdz = -effectiveCamPos.z;
            final double sdist = Math.sqrt(sdx * sdx + sdy * sdy + sdz * sdz);
            if (sdist > 1e-6)
            {
                jobs.add(new RenderJob(sdist, oriented(ps, horizon, () -> SunRenderer.renderRealScale(ps, system.sun(), tick, partialTick, sdx, sdy, sdz, sdist, realCamPos))));
            }
        }

        for (PlanetDefinition planet : system.planets())
        {
            double[] p = planet.currentWorldPosition(tick, partialTick);
            boolean skipBody = ctx.isExcluded(planet.id());

            if (!skipBody)
            {
                boolean isSelf = ctx.isSelf(planet.id());
                float alpha = isSelf ? ctx.selfPlanetAlpha() : 1.0f;

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
                    dx = p[0] - effectiveCamPos.x;
                    dy = p[1] - effectiveCamPos.y;
                    dz = p[2] - effectiveCamPos.z;
                    dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                }

                if (dist >= 1e-6 && (isSelf || visible(planet, projectToSafeShell(dx, dy, dz, dist, Math.max(planet.size(), MIN_APPARENT)))))
                {
                    final double fdx = dx, fdy = dy, fdz = dz, fdist = dist;
                    final float[] fpos = { (float) p[0], (float) p[1], (float) p[2] };
                    final boolean fSelf = isSelf;
                    final float fAlpha = alpha;

                    Runnable draw = () -> renderPlanetBody(ps, planet, fSelf, fAlpha, fpos, fdx, fdy, fdz, fdist, ctx, tick, partialTick, system.sun(), null, animSeconds);
                    jobs.add(new RenderJob(dist, fSelf ? draw : oriented(ps, horizon, draw)));
                }
            }

            for (PlanetDefinition moon : planet.moons())
            {
                double[] mLocal = moon.currentWorldPosition(tick, partialTick);

                final double mdx = p[0] + mLocal[0] - effectiveCamPos.x;
                final double mdy = p[1] + mLocal[1] - effectiveCamPos.y;
                final double mdz = p[2] + mLocal[2] - effectiveCamPos.z;

                final double mdist = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
                if (mdist < 1e-6) continue;
                if (!visible(moon, projectToSafeShell(mdx, mdy, mdz, mdist, Math.max(moon.size(), MIN_APPARENT)))) continue;

                final float[] mpos = { (float) (p[0] + mLocal[0]), (float) (p[1] + mLocal[1]), (float) (p[2] + mLocal[2]) };
                Runnable draw = () -> renderPlanetBody(ps, moon, false, 1.0f, mpos, mdx, mdy, mdz, mdist, ctx, tick, partialTick, system.sun(), planet, animSeconds);
                jobs.add(new RenderJob(mdist, oriented(ps, horizon, draw)));
            }
        }

        jobs.sort((a, b) -> Double.compare(b.distance(), a.distance()));
        for (RenderJob job : jobs) job.draw().run();
    }

    private boolean visible(PlanetDefinition body, Projected proj)
    {
        return frustum.testSphere(proj.dx, proj.dy, proj.dz, cullRadius(body, proj.size));
    }

    private static float cullRadius(PlanetDefinition body, float apparentSize)
    {
        float f = CUBE_CORNER;
        if (body.atmosphere().isPresent()) f = 1.25f;
        if (body.clouds().isPresent()) f = Math.max(f, 1.30f);
        if (body.rings().isPresent()) f = Math.max(f, body.rings().get().outerRadius() * 0.55f);
        return apparentSize * f;
    }

    private static Runnable oriented(PoseStack ps, Quaternionf horizon, Runnable draw)
    {
        if (horizon == null) return draw;
        return () ->
        {
            ps.pushPose();
            ps.mulPose(horizon);
            draw.run();
            ps.popPose();
        };
    }

    private void renderPlanetBody(PoseStack ps, PlanetDefinition planet, boolean isSelf, float alpha, float[] pos, double dx, double dy, double dz, double dist, CelestialFrameContext ctx, long tick, float partialTick, SunDefinition sun, @Nullable PlanetDefinition parent, double animSeconds)
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

        final PlanetEnvironment env = (parent == null) ? PlanetEnvironment.of(sun, planet) : PlanetEnvironment.ofMoon(sun, parent, planet);
        final float sunAngRad = PhysicalScale.sunAngularRadius(sun, env.distanceAu());
        Quaternionf invRot = orientation.conjugate(new Quaternionf());

        Vector3f litFrom = new Vector3f(worldSun);
        if (isSelf && ctx.horizonRotation() != null) ctx.horizonRotation().transform(litFrom);

        Vector3f localSun = invRot.transform(new Vector3f(litFrom));
        Vector3f localCam = invRot.transform(new Vector3f(cdx, cdy, cdz));
        float sunLX = localSun.x, sunLY = localSun.y, sunLZ = localSun.z;
        float camLX = localCam.x, camLY = localCam.y, camLZ = localCam.z;
        final float shellDist = (float) Math.sqrt(proj.dx * proj.dx + proj.dy * proj.dy + proj.dz * proj.dz);
        final float camDistObj = shellDist / apparentSize;

        planet.atmosphere().ifPresent(atmo ->
        {
            ps.pushPose();
            ps.scale(apparentSize, apparentSize, apparentSize);

            GPUProfiler.begin("planet.volumetric.atmosphere");
            AtmosphereRenderer.render(ps, atmo, env, planet.size(), -camLX, -camLY, -camLZ, sunLX, sunLY, sunLZ, camDistObj);
            GPUProfiler.end();

            ps.popPose();
        });

        ps.pushPose();
        ps.scale(apparentSize, apparentSize, apparentSize);

        GPUProfiler.begin("celestial.planet.core");
        BodyRenderer.render(ps, planet.resolveTexture(), alpha, -camLX * camDistObj, -camLY * camDistObj, -camLZ * camDistObj, sunLX, sunLY, sunLZ, planet.rings().orElse(null), sunAngRad, planet.atmosphere().isPresent());
        GPUProfiler.end();

        planet.rings().ifPresent(rings ->
        {
            ps.pushPose();
            ps.scale(1.0f / apparentSize, 1.0f / apparentSize, 1.0f / apparentSize);

            GPUProfiler.begin("celestial.planet.rings");
            RingRenderer.render(ps, rings, apparentSize, -camLX * shellDist, -camLY * shellDist, -camLZ * shellDist, sunLX, sunLY, sunLZ, sunAngRad);
            GPUProfiler.end();

            ps.popPose();
        });

        planet.clouds().ifPresent(clouds ->
        {
            ps.pushPose();

            GPUProfiler.begin("planet.volumetric.clouds");
            CloudsRenderer.render(ps, clouds, -camLX, -camLY, -camLZ, sunLX, sunLY, sunLZ, camDistObj, animSeconds);
            GPUProfiler.end();

            ps.popPose();
        });

        SeamlessGhostSurfacePatchRenderer.renderPatch(ps, planet, -camLX, -camLY, -camLZ);
        ps.popPose();
        ps.popPose();
    }
}