#version 150

in vec3 vObjPos;

uniform vec3 uCamDir;
uniform vec3 uSunDir;
uniform float uCamDist;

uniform float uPlanetHalfExtent;
uniform float uShellThickness;
uniform vec3 uSphereCenter;
uniform float uGeoBlend;
uniform float uShadeCurvature;
uniform float uTerrainGround;
uniform vec3 uGroundColor;
uniform vec3 uRayOrigin;
uniform float uRayleighH;
uniform float uMieH;
uniform float uInvRayleighH;
uniform float uInvMieH;
uniform vec3 uBetaRayleigh;
uniform vec3 uExtRayleigh;
uniform float uBetaMie;
uniform vec3 uMieAlbedo;

uniform float uMieG;
uniform float uSunIntensity;
uniform float uMultiScatterStrength;

out vec4 fragColor;

const int NUM_VIEW_STEPS = 16;
const float INV_VIEW_STEPS = 1.0 / float(NUM_VIEW_STEPS);
const float PI = 3.14159265359;
const float HALF_PI = 1.57079632679;
const float SQRT_PI = 1.77245385091;
const float INV_4PI = 0.07957747155;
const float ERFCX_A = 2.9110;
const float MISS = 1e30;
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float TONEMAP_INV_WHITE_SQ = 1.0 / 36.0;
const vec3 THIRD = vec3(1.0 / 3.0);
const float FIT_B1 = 0.2;
const float FIT_B2 = 0.7;
const float FIT_LIN_B1 = 0.03310;
const float FIT_LIN_B2 = 0.44799;

float sdBox(vec3 p, float halfExtent)
{
    vec3 q = abs(p) - vec3(halfExtent);
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

vec3 boxNormal(vec3 p, float halfExtent)
{
    vec3 o = max(abs(p) - vec3(halfExtent), 0.0);
    float l2 = dot(o, o);
    if (l2 > 1e-14) return sign(p) * o * inversesqrt(l2);
    vec3 a = abs(p);
    return (a.x >= a.y && a.x >= a.z) ? vec3(sign(p.x), 0.0, 0.0) : (a.y >= a.z ? vec3(0.0, sign(p.y), 0.0) : vec3(0.0, 0.0, sign(p.z)));
}

vec3 safeInverse(vec3 d)
{
    return 1.0 / vec3(abs(d.x) < 1e-6 ? 1e-6 : d.x, abs(d.y) < 1e-6 ? 1e-6 : d.y, abs(d.z) < 1e-6 ? 1e-6 : d.z);
}

vec2 rayBox(vec3 origin, vec3 invDir, float halfExtent)
{
    vec3 t0 = (-vec3(halfExtent) - origin) * invDir;
    vec3 t1 = ( vec3(halfExtent) - origin) * invDir;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    return vec2(max(max(tmin.x, tmin.y), tmin.z), min(min(tmax.x, tmax.y), tmax.z));
}

vec2 raySphere(vec3 rel, vec3 dir, float radius)
{
    float b = dot(rel, dir);
    float disc = b * b - (dot(rel, rel) - radius * radius);
    if (disc < 0.0) return vec2(MISS, -MISS);
    float s = sqrt(disc);
    return vec2(-b - s, -b + s);
}

float geometry(vec3 p, float planetHalf, out vec3 n, out bool onSphere)
{
    float altB = sdBox(p, planetHalf);
    vec3 nB = normalize(mix(boxNormal(p, planetHalf), normalize(p), uShadeCurvature));
    onSphere = false;
    if (uGeoBlend >= 1.0) { n = nB; return altB; }

    vec3 rel = p - uSphereCenter;
    float r = length(rel);
    float altS = r - planetHalf;
    onSphere = altS >= altB;
    float altC = onSphere ? altS : altB;
    vec3 nC = onSphere ? rel / r : nB;
    if (uGeoBlend <= 0.0) { n = nC; return altC; }

    n = normalize(mix(nC, nB, uGeoBlend));
    return mix(altC, altB, uGeoBlend);
}

float sunlit(vec3 p, float planetHalf, bool onSphere)
{
    if (!onSphere) return 1.0;
    vec3 rel = p - uSphereCenter;
    float b = dot(rel, uSunDir);
    float disc = b * b - (dot(rel, rel) - planetHalf * planetHalf);
    float litS = (disc > 0.0 && -b - sqrt(disc) > 0.0) ? 0.0 : 1.0;
    return mix(litS, 1.0, uGeoBlend);
}

float chapmanUpper(float x, float cosZ)
{
    float y = sqrt(0.5 * x) * cosZ;
    return sqrt(HALF_PI * x) * ERFCX_A / ((ERFCX_A - 1.0) * SQRT_PI * y + sqrt(PI * y * y + ERFCX_A * ERFCX_A));
}

float sunOpticalDepth(float scaleH, float invScaleH, float planetR, float h, float cosZ)
{
    float x = (planetR + h) * invScaleH;
    float hs = h * invScaleH;
    if (cosZ >= 0.0) return scaleH * exp(-hs) * chapmanUpper(x, cosZ);

    float s = sqrt(max(1.0 - cosZ * cosZ, 0.0));
    if ((planetR + h) * s < planetR) return 1e4;
    return max(scaleH * (2.0 * sqrt(HALF_PI * x * s) * exp(min(x * (1.0 - s) - hs, 80.0)) - exp(-hs) * chapmanUpper(x, -cosZ)), 0.0);
}

float stepWarp(float u, float mode)
{
    if (mode < 0.5) return u * u;
    if (mode < 1.5) { float v = 1.0 - u; return 1.0 - v * v; }
    return u;
}

vec3 linearToSrgb(vec3 c)
{
    c = clamp(c, 0.0, 1.0);
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c));
}

void main()
{
    float planetHalf = uPlanetHalfExtent;
    float outerHalf = planetHalf + uShellThickness;
    vec3 camPos = uCamDir * uCamDist;
    vec3 rayDir = normalize(vObjPos - camPos);
    vec3 origin = uRayOrigin;
    vec3 invRay = safeInverse(rayDir);

    vec2 groundHit = rayBox(origin, invRay, planetHalf);
    bool overGround = groundHit.x > 0.0 && groundHit.x < groundHit.y;
    float ground = overGround ? groundHit.x : MISS;
    vec2 shellBox = rayBox(origin, invRay, outerHalf);
    float entry = shellBox.x < shellBox.y ? shellBox.x : MISS;
    float tExit = shellBox.x < shellBox.y ? shellBox.y : -MISS;
    if (uGeoBlend < 1.0)
    {
        vec2 shellSphere = raySphere(origin - uSphereCenter, rayDir, outerHalf);
        entry = min(entry, shellSphere.x);
        tExit = uGeoBlend <= 0.0 ? (overGround ? max(shellSphere.y, ground) : shellSphere.y) : max(tExit, shellSphere.y);
    }

    float tStart = max(entry, 0.0);
    float tEnd = min(tExit, ground);
    if (tEnd <= tStart) discard;

    bool inside = entry <= 0.0;
    float mode = inside ? 0.0 : (overGround ? 1.0 : 2.0);
    float len = tEnd - tStart;

    vec2 viewDepth = vec2(0.0);
    vec3 sumR = vec3(0.0);
    vec3 sumM = vec3(0.0);

    for (int i = 0; i < NUM_VIEW_STEPS; i++)
    {
        float u0 = float(i) * INV_VIEW_STEPS;
        float u1 = u0 + INV_VIEW_STEPS;
        float us = u0 + 0.5 * INV_VIEW_STEPS;
        float f0 = stepWarp(u0, mode), f1 = stepWarp(u1, mode);
        float t = tStart + len * stepWarp(us, mode);
        float dt = len * (f1 - f0);
        float into = (stepWarp(us, mode) - f0) / max(f1 - f0, 1e-7);

        vec3 p = origin + rayDir * t;
        vec3 n;
        bool onSphere;
        float h = max(geometry(p, planetHalf, n, onSphere), 0.0);
        vec2 d = vec2(exp(-h * uInvRayleighH), exp(-h * uInvMieH));
        vec2 toSample = viewDepth + d * (into * dt);
        viewDepth += d * dt;

        float lit = sunlit(p, planetHalf, onSphere);
        if (lit > 0.0)
        {
            float cosZ = dot(n, uSunDir);
            vec2 sunDepth = vec2(sunOpticalDepth(uRayleighH, uInvRayleighH, planetHalf, h, cosZ), sunOpticalDepth(uMieH, uInvMieH, planetHalf, h, cosZ));
            vec2 total = toSample + sunDepth;
            vec3 tr = exp(-(uExtRayleigh * total.x + vec3(uBetaMie * total.y))) * (lit * dt);
            sumR += d.x * tr;
            sumM += d.y * tr;
        }
    }

    float cosTheta = dot(rayDir, uSunDir);
    float rayleighPhase = 3.0 / (16.0 * PI) * (1.0 + cosTheta * cosTheta);
    float g = clamp(uMieG, -0.99, 0.99);
    float g2 = g * g;
    float miePhase = (1.0 - g2) * INV_4PI / pow(max(1e-4, 1.0 + g2 - 2.0 * g * cosTheta), 1.5);

    float iso = uMultiScatterStrength * INV_4PI;
    vec3 radiance = uSunIntensity * (uBetaRayleigh * sumR * (rayleighPhase + iso) + (uBetaMie * uMieAlbedo) * sumM * (miePhase + iso));
    float lum = dot(radiance, LUMA);
    vec3 lt = clamp(radiance * ((1.0 + lum * TONEMAP_INV_WHITE_SQ) / (1.0 + lum)), 0.0, 1.0);
    vec3 transmittance = exp(-(uExtRayleigh * viewDepth.x + vec3(uBetaMie * viewDepth.y)));

    vec3 col;
    float alpha;
    if (uTerrainGround > 0.5 && overGround)
    {
        col = linearToSrgb(lt + transmittance * uGroundColor);
        alpha = 1.0;
    }
    else if (overGround)
    {
        vec3 y1 = linearToSrgb(lt + transmittance * FIT_LIN_B1);
        vec3 y2 = linearToSrgb(lt + transmittance * FIT_LIN_B2);
        float k = dot(y2 - y1, THIRD) / (FIT_B2 - FIT_B1);
        col = max(y1 - k * FIT_B1, 0.0);
        alpha = 1.0 - k;
    }
    else
    {
        col = linearToSrgb(lt);
        alpha = 1.0 - dot(linearToSrgb(lt + transmittance) - col, THIRD);
    }

    fragColor = vec4(col, alpha);
}
