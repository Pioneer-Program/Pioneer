package cute.ame.pioneer.SkyPlanet.Rendering.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Core.Render.Helper.CubeGeometry;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.AtmosphereDefinition;
import cute.ame.pioneer.SkyPlanet.Physics.AtmosphericPhysics;
import cute.ame.pioneer.SkyPlanet.Physics.PlanetEnvironment;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.Map;

import static cute.ame.pioneer.Core.Render.Helper.UniformHelper.set;

public final class AtmosphereRenderer
{
    private static final ResourceLocation ATMOSPHERE_RENDER_TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "atmosphere");

    static
    {
        VeilSkyShaderHelper.registerVfxShader(ATMOSPHERE_RENDER_TYPE);
    }

    private static final float MAX_CAM_DIST_OBJ = 64.0f;
    private static final float MULTI_SCATTER = 0.6f;
    private static final double SKY_EXPOSURE = 28.0;
    private static final double SPACE_EXPOSURE = Math.PI;
    private static final int CACHE_LIMIT = 64;
    private static final float BLEND_START_SHELLS = 0.05f, BLEND_END_SHELLS = 0.2f;
    private static final double MIN_ORIGIN_ALTITUDE_M = 2.0;
    private static final long PROBE_STALE_NANOS = 250_000_000L;
    private static long skyFrameNanos = Long.MIN_VALUE;
    private static boolean probeValid;
    private static Coeffs probeCoeffs;
    private static float probeSunIntensity, probeMieG, probeBlend, probeHalf;
    private static final Vector3f probeCam = new Vector3f(), probeSun = new Vector3f(), probeCenter = new Vector3f();
    private static final Matrix3f probeViewToObj = new Matrix3f();


    private record Coeffs(float shell, float hR, float hM, float bRr, float bRg, float bRb, float eRr, float eRg, float eRb, float bM, float albR, float albG, float albB, float irradiance) {}

    private static final Map<AtmosphereDefinition, Coeffs> CACHE = new IdentityHashMap<>();

    private static Coeffs coeffs(AtmosphereDefinition atmo, PlanetEnvironment env, float radiusKm)
    {
        Coeffs c = CACHE.get(atmo);
        if (c != null) return c;
        if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();

        final double tempK = env.surfaceTempK();
        final double gravity = env.gravityMs2();
        final double shell = Math.max(atmo.shellScale(tempK, gravity, radiusKm) - 1.0, 1e-5);
        final double hR = atmo.rayleighScaleHeightFrac(tempK, gravity, radiusKm) * shell;
        final double hM = atmo.mieScaleHeightFrac(tempK, gravity, radiusKm) * shell;

        final double colR = hR * -Math.expm1(-shell / hR);
        final double colM = hM * -Math.expm1(-shell / hM);

        final double[] tauR = AtmosphericPhysics.scatteringDepthRgb(atmo.composition(), atmo.surfacePressureBar(), gravity);
        final double[] tauO = AtmosphericPhysics.ozoneDepthRgb(atmo.ozoneStrength());
        final double tauM = Math.max(atmo.hazeOpticalDepth(), 0.0);
        final float[] alb = atmo.hazeRgb();

        c = new Coeffs(
            (float) shell, (float) hR, (float) hM,
            (float) (tauR[0] / colR), (float) (tauR[1] / colR), (float) (tauR[2] / colR),
            (float) ((tauR[0] + tauO[0]) / colR), (float) ((tauR[1] + tauO[1]) / colR), (float) ((tauR[2] + tauO[2]) / colR),
            (float) (tauM / colM),
            alb[0], alb[1], alb[2],
            (float) env.irradianceRelative());

        CACHE.put(atmo, c);
        return c;
    }

    public static void render(PoseStack poseStack, AtmosphereDefinition atmo, PlanetEnvironment env, float radiusKm, float camDirX, float camDirY, float camDirZ, float sunDirX, float sunDirY, float sunDirZ, float camDistObj, boolean terrainGround, float terrainCutoff)
    {
        final Coeffs c = coeffs(atmo, env, radiusKm);
        final float clampedCamDist = Math.min(camDistObj, MAX_CAM_DIST_OBJ);
        final float planetHalf = 1.0f;
        final float outerHalf = planetHalf + c.shell();
        final float px = camDirX * clampedCamDist, py = camDirY * clampedCamDist, pz = camDirZ * clampedCamDist;
        final float gx = Math.clamp(px, -planetHalf, planetHalf), gy = Math.clamp(py, -planetHalf, planetHalf), gz = Math.clamp(pz, -planetHalf, planetHalf);
        float nx = px - gx, ny = py - gy, nz = pz - gz;
        final float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl > 1e-6f) { nx /= nl; ny /= nl; nz /= nl; }
        else
        {
            final float ax = Math.abs(px), ay = Math.abs(py), az = Math.abs(pz);
            nx = (ax >= ay && ax >= az) ? Math.signum(px) : 0.0f;
            ny = (nx == 0.0f && ay >= az) ? Math.signum(py) : 0.0f;
            nz = (nx == 0.0f && ny == 0.0f) ? Math.signum(pz) : 0.0f;
        }
        final float cx = gx - nx * planetHalf, cy = gy - ny * planetHalf, cz = gz - nz * planetHalf;

        final float camAlt = sdBox(px, py, pz, planetHalf);
        final float minAlt = (float) (MIN_ORIGIN_ALTITUDE_M / (Math.max(radiusKm, 1e-3) * 1000.0));
        final float lift = Math.max(minAlt - camAlt, 0.0f);
        final float ox = px + nx * lift, oy = py + ny * lift, oz = pz + nz * lift;
        final float blend = smoothstep(BLEND_START_SHELLS * c.shell(), BLEND_END_SHELLS * c.shell(), camAlt);
        final double adapt = blend;
        final float sunIntensity = (float) (Math.exp(Math.log(SKY_EXPOSURE) + (Math.log(SPACE_EXPOSURE) - Math.log(SKY_EXPOSURE)) * adapt) * c.irradiance());
        final float proxyHalf = blend < 1.0f ? Math.max(Math.abs(cx), Math.max(Math.abs(cy), Math.abs(cz))) + outerHalf : outerHalf;
        final Matrix4f planetModel = new Matrix4f(poseStack.last().pose());

        final float[] fogColor = RenderSystem.getShaderFogColor();
        final float[] fog = { fogColor[0], fogColor[1], fogColor[2] };
        final float groundR = srgbToLinear(fog[0]), groundG = srgbToLinear(fog[1]), groundB = srgbToLinear(fog[2]);

        if (blend < 1.0f) publishProbe(c, sunIntensity, atmo.mieG(), ox, oy, oz, sunDirX, sunDirY, sunDirZ, cx, cy, cz, blend, planetHalf, planetModel);

        VeilSkyShaderHelper.draw(
        ATMOSPHERE_RENDER_TYPE,
        shader ->
        {
            set(shader, "uCamDir", camDirX, camDirY, camDirZ);
            set(shader, "uSunDir", sunDirX, sunDirY, sunDirZ);
            set(shader, "uCamDist", clampedCamDist);
            set(shader, "uPlanetHalfExtent", planetHalf);
            set(shader, "uShellThickness", c.shell());
            set(shader, "uSphereCenter", cx, cy, cz);
            set(shader, "uGeoBlend", blend);
            set(shader, "uTerrainGround", terrainGround ? 1.0f : 0.0f);
            set(shader, "uRayOrigin", ox, oy, oz);
            set(shader, "uGroundColor", groundR, groundG, groundB);
            set(shader, "uGroundColorDisplay", fog[0], fog[1], fog[2]);
            set(shader, "uTerrainCutoff", terrainGround ? terrainCutoff : 0.0f);
            set(shader, "uSqrtMieRatio", (float) Math.sqrt(c.hR() / c.hM()));
            set(shader, "uRayleighH", c.hR());
            set(shader, "uMieH", c.hM());
            set(shader, "uInvRayleighH", 1.0f / c.hR());
            set(shader, "uInvMieH", 1.0f / c.hM());
            set(shader, "uBetaRayleigh", c.bRr(), c.bRg(), c.bRb());
            set(shader, "uExtRayleigh", c.eRr(), c.eRg(), c.eRb());
            set(shader, "uBetaMie", c.bM());
            set(shader, "uMieAlbedo", c.albR(), c.albG(), c.albB());
            set(shader, "uMieG", atmo.mieG());
            set(shader, "uSunIntensity", sunIntensity);
            set(shader, "uShadeCurvature", BodyRenderer.CURVATURE);
            set(shader, "uMultiScatterStrength", MULTI_SCATTER);
            set(shader, "uPlanetModel", planetModel);
        },
        renderType ->
        {
            BufferBuilder buf = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
            CubeGeometry.emit(buf, proxyHalf * 2.0f);
            MeshData mesh = buf.buildOrThrow();

            renderType.setupRenderState();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            BufferUploader.drawWithShader(mesh);
            renderType.clearRenderState();
        });

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    public static void beginSkyFrame()
    {
        skyFrameNanos = System.nanoTime();
        probeValid = false;
    }

    private static void publishProbe(Coeffs c, float sunIntensity, float mieG, float px, float py, float pz, float sx, float sy, float sz, float cx, float cy, float cz, float blend, float half, Matrix4f planetModel)
    {
        probeValid = true;
        probeCoeffs = c;
        probeSunIntensity = sunIntensity;
        probeMieG = mieG;
        probeBlend = blend;
        probeHalf = half;
        probeCam.set(px, py, pz);
        probeSun.set(sx, sy, sz);
        probeCenter.set(cx, cy, cz);
        planetModel.get3x3(probeViewToObj).invert();
    }

    public static boolean horizonFogColor(float[] out)
    {
        if (System.nanoTime() - skyFrameNanos > PROBE_STALE_NANOS) return false;
        if (!probeValid) { out[0] = out[1] = out[2] = 0.0f; return true; }

        final Coeffs c = probeCoeffs;
        final Vector3f up = new Vector3f();
        geometry(probeCam, up);

        final Vector3f dir = probeViewToObj.transform(new Vector3f(0.0f, 0.0f, -1.0f)).normalize();
        dir.fma(-dir.dot(up), up);
        if (dir.lengthSquared() < 1e-8f)
        {
            dir.set(Math.abs(up.x) < 0.9f ? 1.0f : 0.0f, Math.abs(up.x) < 0.9f ? 0.0f : 1.0f, 0.0f);
            dir.fma(-dir.dot(up), up);
        }
        dir.normalize();

        final float outer = probeHalf + c.shell();
        final float tEnd = Math.max(raySphereExit(probeCam.x - probeCenter.x, probeCam.y - probeCenter.y, probeCam.z - probeCenter.z, dir, outer), 0.0f);
        final float invHR = 1.0f / c.hR(), invHM = 1.0f / c.hM();

        double vdR = 0.0, vdM = 0.0;
        double sR0 = 0.0, sR1 = 0.0, sR2 = 0.0, sM0 = 0.0, sM1 = 0.0, sM2 = 0.0;
        final Vector3f p = new Vector3f(), n = new Vector3f();
        for (int i = 0; i < VIEW_STEPS; i++)
        {
            final float u0 = i * INV_VIEW_STEPS, u1 = u0 + INV_VIEW_STEPS, um = u0 + 0.5f * INV_VIEW_STEPS;
            final float t = tEnd * um * um, dt = tEnd * (u1 * u1 - u0 * u0);
            probeCam.fma(t, dir, p);
            final float h = Math.max(geometry(p, n), 0.0f);
            final double dR = Math.exp(-h * invHR), dM = Math.exp(-h * invHM);
            final double toR = vdR + dR * 0.5 * dt, toM = vdM + dM * 0.5 * dt;
            vdR += dR * dt;
            vdM += dM * dt;

            final float lit = sunlit(p);
            if (lit <= 0.0f) continue;
            final float cosZ = n.dot(probeSun);
            final double totR = toR + sunOpticalDepth(c.hR(), invHR, probeHalf, h, cosZ);
            final double totM = toM + sunOpticalDepth(c.hM(), invHM, probeHalf, h, cosZ);
            final double w = lit * dt, mie = c.bM() * totM;
            final double t0 = Math.exp(-(c.eRr() * totR + mie)) * w, t1 = Math.exp(-(c.eRg() * totR + mie)) * w, t2 = Math.exp(-(c.eRb() * totR + mie)) * w;
            sR0 += dR * t0; sR1 += dR * t1; sR2 += dR * t2;
            sM0 += dM * t0; sM1 += dM * t1; sM2 += dM * t2;
        }

        final double cosT = dir.dot(probeSun);
        final double rayleighPhase = 3.0 / (16.0 * Math.PI) * (1.0 + cosT * cosT);
        final double g = Math.clamp(probeMieG, -0.99, 0.99), g2 = g * g;
        final double miePhase = (1.0 - g2) * INV_4PI / Math.pow(Math.max(1e-4, 1.0 + g2 - 2.0 * g * cosT), 1.5);
        final double iso = MULTI_SCATTER * INV_4PI, pr = rayleighPhase + iso, pm = miePhase + iso, sun = probeSunIntensity;

        final double l0 = sun * (c.bRr() * sR0 * pr + c.bM() * c.albR() * sM0 * pm);
        final double l1 = sun * (c.bRg() * sR1 * pr + c.bM() * c.albG() * sM1 * pm);
        final double l2 = sun * (c.bRb() * sR2 * pr + c.bM() * c.albB() * sM2 * pm);
        final double lum = 0.2126 * l0 + 0.7152 * l1 + 0.0722 * l2;
        final double k = (1.0 + lum * TONEMAP_INV_WHITE_SQ) / (1.0 + lum);
        out[0] = linearToSrgb(l0 * k);
        out[1] = linearToSrgb(l1 * k);
        out[2] = linearToSrgb(l2 * k);
        return true;
    }

    private static final int VIEW_STEPS = 16;
    private static final float INV_VIEW_STEPS = 1.0f / VIEW_STEPS;
    private static final double INV_4PI = 1.0 / (4.0 * Math.PI);
    private static final double TONEMAP_INV_WHITE_SQ = 1.0 / 36.0;
    private static final double ERFCX_A = 2.9110, SQRT_PI = Math.sqrt(Math.PI);

    private static boolean probeOnSphere;

    private static float geometry(Vector3f p, Vector3f nOut)
    {
        final float qx = Math.abs(p.x) - probeHalf, qy = Math.abs(p.y) - probeHalf, qz = Math.abs(p.z) - probeHalf;
        final float ox = Math.max(qx, 0.0f), oy = Math.max(qy, 0.0f), oz = Math.max(qz, 0.0f);
        final float ol = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
        final float altB = ol + Math.min(Math.max(qx, Math.max(qy, qz)), 0.0f);
        final Vector3f nB = (ol > 1e-7f ? new Vector3f(Math.signum(p.x) * ox / ol, Math.signum(p.y) * oy / ol, Math.signum(p.z) * oz / ol) : dominantAxis(p))
            .lerp(new Vector3f(p).normalize(), BodyRenderer.CURVATURE).normalize();
        probeOnSphere = false;
        if (probeBlend >= 1.0f) { nOut.set(nB); return altB; }

        final float rx = p.x - probeCenter.x, ry = p.y - probeCenter.y, rz = p.z - probeCenter.z;
        final float r = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        final float altS = r - probeHalf;
        probeOnSphere = altS >= altB;
        final float altC = probeOnSphere ? altS : altB;
        final Vector3f nC = probeOnSphere ? new Vector3f(rx / r, ry / r, rz / r) : nB;
        if (probeBlend <= 0.0f) { nOut.set(nC); return altC; }

        nOut.set(nC).lerp(nB, probeBlend).normalize();
        return altC + (altB - altC) * probeBlend;
    }

    private static Vector3f dominantAxis(Vector3f p)
    {
        final float ax = Math.abs(p.x), ay = Math.abs(p.y), az = Math.abs(p.z);
        if (ax >= ay && ax >= az) return new Vector3f(Math.signum(p.x), 0.0f, 0.0f);
        return ay >= az ? new Vector3f(0.0f, Math.signum(p.y), 0.0f) : new Vector3f(0.0f, 0.0f, Math.signum(p.z));
    }

    private static float sunlit(Vector3f p)
    {
        if (!probeOnSphere) return 1.0f;
        final float rx = p.x - probeCenter.x, ry = p.y - probeCenter.y, rz = p.z - probeCenter.z;
        final float b = rx * probeSun.x + ry * probeSun.y + rz * probeSun.z;
        final float disc = b * b - (rx * rx + ry * ry + rz * rz - probeHalf * probeHalf);
        final float litS = (disc > 0.0f && -b - (float) Math.sqrt(disc) > 0.0f) ? 0.0f : 1.0f;
        return litS + (1.0f - litS) * probeBlend;
    }

    private static float raySphereExit(float rx, float ry, float rz, Vector3f d, float radius)
    {
        final float b = rx * d.x + ry * d.y + rz * d.z;
        final float disc = b * b - (rx * rx + ry * ry + rz * rz - radius * radius);
        return disc < 0.0f ? 0.0f : -b + (float) Math.sqrt(disc);
    }

    private static double chapmanUpper(double x, double cosZ)
    {
        final double y = Math.sqrt(0.5 * x) * cosZ;
        return Math.sqrt(0.5 * Math.PI * x) * ERFCX_A / ((ERFCX_A - 1.0) * SQRT_PI * y + Math.sqrt(Math.PI * y * y + ERFCX_A * ERFCX_A));
    }

    private static double sunOpticalDepth(double scaleH, double invScaleH, double planetR, double h, double cosZ)
    {
        final double x = (planetR + h) * invScaleH, hs = h * invScaleH;
        if (cosZ >= 0.0) return scaleH * Math.exp(-hs) * chapmanUpper(x, cosZ);
        final double s = Math.sqrt(Math.max(1.0 - cosZ * cosZ, 0.0));
        if ((planetR + h) * s < planetR) return 1e4;
        return Math.max(scaleH * (2.0 * Math.sqrt(0.5 * Math.PI * x * s) * Math.exp(Math.min(x * (1.0 - s) - hs, 80.0)) - Math.exp(-hs) * chapmanUpper(x, -cosZ)), 0.0);
    }

    private static float srgbToLinear(float v)
    {
        final double c = Math.clamp(v, 0.0, 1.0);
        return (float) (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
    }

    private static float linearToSrgb(double v)
    {
        final double c = Math.clamp(v, 0.0, 1.0);
        return (float) (c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    private static float sdBox(float x, float y, float z, float half)
    {
        final float qx = Math.abs(x) - half, qy = Math.abs(y) - half, qz = Math.abs(z) - half;
        final float ox = Math.max(qx, 0.0f), oy = Math.max(qy, 0.0f), oz = Math.max(qz, 0.0f);
        return (float) Math.sqrt(ox * ox + oy * oy + oz * oz) + Math.min(Math.max(qx, Math.max(qy, qz)), 0.0f);
    }

    private static float smoothstep(float e0, float e1, float x)
    {
        final float t = Math.clamp((x - e0) / (e1 - e0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
