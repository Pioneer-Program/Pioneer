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
uniform float uTerrainCutoff;
uniform vec3 uGroundColor;
uniform vec3 uGroundColorDisplay;
uniform vec3 uRayOrigin;
uniform float uRayleighH;
uniform float uMieH;
uniform float uInvRayleighH;
uniform float uInvMieH;
uniform float uSqrtMieRatio;
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
const float SQRT_HALF = 0.70710678119;
const float SQRT_HALF_PI = 1.25331413732;
const float INV_4PI = 0.07957747155;
const float ERFCX_A = 2.9110;
const float ERFCX_A2 = ERFCX_A * ERFCX_A;
const float ERFCX_K = (ERFCX_A - 1.0) * SQRT_PI;
const float MISS = 1e30;
const float OCCULTED = 1e4;
const float OPAQUE_DEPTH = 7.0;
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

vec3 curvedBoxNormal(vec3 p, float halfExtent)
{
    vec3 o = max(abs(p) - vec3(halfExtent), 0.0);
    float l2 = dot(o, o);
    vec3 n;
    if (l2 > 1e-14) n = sign(p) * o * inversesqrt(l2);
    else
    {
        vec3 a = abs(p);
        n = (a.x >= a.y && a.x >= a.z) ? vec3(sign(p.x), 0.0, 0.0) : (a.y >= a.z ? vec3(0.0, sign(p.y), 0.0) : vec3(0.0, 0.0, sign(p.z)));
    }
    return normalize(mix(n, p * inversesqrt(dot(p, p)), uShadeCurvature));
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

float geometry(vec3 p, float planetHalf, out float cosZ)
{
    float altB = sdBox(p, planetHalf);
    if (uGeoBlend >= 1.0)
    {
        cosZ = dot(curvedBoxNormal(p, planetHalf), uSunDir);
        return altB;
    }

    vec3 rel = p - uSphereCenter;
    float r = length(rel);
    float altS = r - planetHalf;
    bool onSphere = altS >= altB;
    float altC = max(altS, altB);
    if (uGeoBlend <= 0.0)
    {
        cosZ = onSphere ? dot(rel, uSunDir) / r : dot(curvedBoxNormal(p, planetHalf), uSunDir);
        return altC;
    }

    vec3 nB = curvedBoxNormal(p, planetHalf);
    vec3 nC = onSphere ? rel / r : nB;
    cosZ = dot(normalize(mix(nC, nB, uGeoBlend)), uSunDir);
    return mix(altC, altB, uGeoBlend);
}

vec2 chapmanUpper(vec2 sqrtX, float cosZ)
{
    vec2 y = SQRT_HALF * sqrtX * cosZ;
    return SQRT_HALF_PI * sqrtX * ERFCX_A / (ERFCX_K * y + sqrt(PI * y * y + ERFCX_A2));
}

vec2 sunOpticalDepth(float planetR, float h, float cosZ, vec2 dens)
{
    float r = planetR + h;
    float sx = sqrt(r * uInvRayleighH);
    vec2 sqrtX = vec2(sx, sx * uSqrtMieRatio);
    vec2 scaleH = vec2(uRayleighH, uMieH);
    if (cosZ >= 0.0) return scaleH * dens * chapmanUpper(sqrtX, cosZ);

    float s = sqrt(max(1.0 - cosZ * cosZ, 0.0));
    if (r * s < planetR) return vec2(OCCULTED);
    vec2 x = sqrtX * sqrtX;
    vec2 hs = h * vec2(uInvRayleighH, uInvMieH);
    return max(scaleH * (2.0 * sqrt(HALF_PI * s * x) * exp(min(x * (1.0 - s) - hs, vec2(80.0))) - dens * chapmanUpper(sqrtX, -cosZ)), 0.0);
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
    vec3 rayDir = normalize(vObjPos - uCamDir * uCamDist);
    vec3 origin = uRayOrigin;
    vec3 invRay = safeInverse(rayDir);

    vec2 groundHit = rayBox(origin, invRay, planetHalf);
    bool overGround = groundHit.x > 0.0 && groundHit.x < groundHit.y;
    float ground = overGround ? groundHit.x : MISS;

    bool terrain = uTerrainGround > 0.5;
    if (terrain && ground < uTerrainCutoff)
    {
        fragColor = vec4(uGroundColorDisplay, 1.0);
        return;
    }

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
    float warpA = inside ? 1.0 : (overGround ? -1.0 : 0.0);
    float warpB = inside ? 0.0 : (overGround ? 2.0 : 1.0);
    float len = tEnd - tStart;
    vec2 invH = vec2(uInvRayleighH, uInvMieH);
    float minExt = min(min(uExtRayleigh.x, uExtRayleigh.y), uExtRayleigh.z);

    vec2 viewDepth = vec2(0.0);
    vec3 sumR = vec3(0.0);
    vec3 sumM = vec3(0.0);
    float f0 = 0.0;
    for (int i = 0; i < NUM_VIEW_STEPS; i++)
    {
        float u1 = float(i + 1) * INV_VIEW_STEPS;
        float um = u1 - 0.5 * INV_VIEW_STEPS;
        float f1 = (warpA * u1 + warpB) * u1;
        float fm = (warpA * um + warpB) * um;
        float df = f1 - f0;
        float dt = len * df;

        vec3 p = origin + rayDir * (tStart + len * fm);
        float cosZ;
        float h = max(geometry(p, planetHalf, cosZ), 0.0);
        vec2 d = exp(-h * invH);
        vec2 toSample = viewDepth + d * (dt * (fm - f0) / max(df, 1e-7));
        viewDepth += d * dt;

        vec2 sunDepth = sunOpticalDepth(planetHalf, h, cosZ, d);
        if (sunDepth.x < OCCULTED)
        {
            vec2 total = toSample + sunDepth;
            vec3 tr = exp(-(uExtRayleigh * total.x + vec3(uBetaMie * total.y))) * dt;
            sumR += d.x * tr;
            sumM += d.y * tr;
        }

        if (minExt * viewDepth.x + uBetaMie * viewDepth.y > OPAQUE_DEPTH) break;
        f0 = f1;
    }

    float cosTheta = dot(rayDir, uSunDir);
    float rayleighPhase = 3.0 / (16.0 * PI) * (1.0 + cosTheta * cosTheta);
    float g = clamp(uMieG, -0.99, 0.99);
    float g2 = g * g;
    float denom = max(1e-4, 1.0 + g2 - 2.0 * g * cosTheta);
    float miePhase = (1.0 - g2) * INV_4PI / (denom * sqrt(denom));

    float iso = uMultiScatterStrength * INV_4PI;
    vec3 radiance = uSunIntensity * (uBetaRayleigh * sumR * (rayleighPhase + iso) + (uBetaMie * uMieAlbedo) * sumM * (miePhase + iso));
    float lum = dot(radiance, LUMA);
    vec3 lt = clamp(radiance * ((1.0 + lum * TONEMAP_INV_WHITE_SQ) / (1.0 + lum)), 0.0, 1.0);
    vec3 transmittance = exp(-(uExtRayleigh * viewDepth.x + vec3(uBetaMie * viewDepth.y)));

    vec3 col;
    float alpha;
    if (terrain && overGround)
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
