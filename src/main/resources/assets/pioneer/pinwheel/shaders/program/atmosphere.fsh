#version 150

in vec3 vObjPos;

uniform vec3 uColor;
uniform float uFresnelPower;
uniform float uOpacity;

uniform vec3 uCamDir;
uniform vec3 uSunDir;
uniform float uCamDist;

uniform float uPlanetHalfExtent;
uniform float uShellThickness;
uniform float uAtmoBoundRadius;

uniform float uRayleighScaleHeight;
uniform float uMieScaleHeight;
uniform float uMieG;
uniform float uMieStrength;
uniform float uSunIntensity;
uniform float uOzoneStrength;
uniform float uMultiScatterStrength;

out vec4 fragColor;

const int NUM_VIEW_STEPS  = 12;
const int NUM_SUN_STEPS   = 6;
const float PI = 3.14159265359;

const float RAYLEIGH_STRENGTH = 9.0;
const float MIE_STRENGTH_BASE = 4.0;

float clamp01(float v) { return clamp(v, 0.0, 1.0); }
float luminance(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

float sdBox(vec3 p, float halfExtent)
{
    vec3 q = abs(p) - vec3(halfExtent);
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

vec2 raySphere(vec3 origin, vec3 dir, float radius)
{
    float b = dot(origin, dir);
    float c = dot(origin, origin) - radius * radius;
    float disc = b * b - c;
    if (disc < 0.0) return vec2(1.0, -1.0);
    float s = sqrt(disc);
    return vec2(-b - s, -b + s);
}

vec2 rayBox(vec3 origin, vec3 dir, float halfExtent)
{
    vec3 safeDir = vec3(abs(dir.x) < 1e-6 ? 1e-6 : dir.x, abs(dir.y) < 1e-6 ? 1e-6 : dir.y, abs(dir.z) < 1e-6 ? 1e-6 : dir.z);
    vec3 invDir = 1.0 / safeDir;
    vec3 t0 = (-vec3(halfExtent) - origin) * invDir;
    vec3 t1 = ( vec3(halfExtent) - origin) * invDir;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    float tNear = max(max(tmin.x, tmin.y), tmin.z);
    float tFar  = min(min(tmax.x, tmax.y), tmax.z);
    return vec2(tNear, tFar);
}

float densityAt(float altitude, float shellThickness, float scaleHeightFrac)
{
    float h = max(altitude, 0.0) / max(shellThickness, 1e-5);
    float scaleHeight = max(scaleHeightFrac, 1e-3);
    return exp(-h / scaleHeight);
}

vec3 ozoneAbsorption(float altitude, float shellThickness)
{
    float h = clamp01(altitude / max(shellThickness, 1e-5));
    float band = clamp01(1.0 - abs(h - 0.55) / 0.35);
    band = band * band;
    return vec3(0.65, 0.9, 0.15) * band;
}

vec3 sunOpticalDepth(vec3 origin, vec3 sunDir, float planetHalf, float atmoBoundR, float shellThickness, float rayleighSH, float mieSH)
{
    vec2 boxHit = rayBox(origin, sunDir, planetHalf);
    bool sunBlocked = boxHit.x < boxHit.y && boxHit.x > 1e-4;
    if (sunBlocked) return vec3(1e6);

    vec2 hit = raySphere(origin, sunDir, atmoBoundR);
    float sunRayLen = max(hit.y, 0.0);

    float stepSize = sunRayLen / float(NUM_SUN_STEPS);
    vec3 depth = vec3(0.0);
    for (int i = 0; i < NUM_SUN_STEPS; i++)
    {
        vec3 samplePos = origin + sunDir * (stepSize * (float(i) + 0.5));
        float altitude = sdBox(samplePos, planetHalf);
        depth.x += densityAt(altitude, shellThickness, rayleighSH) * stepSize;
        depth.y += densityAt(altitude, shellThickness, mieSH) * stepSize;
        depth.z += ozoneAbsorption(altitude, shellThickness).g * stepSize;
    }
    return depth;
}

void main()
{
    vec3 camPos = uCamDir * uCamDist;
    vec3 rayDir = normalize(vObjPos - camPos);

    float planetHalf = uPlanetHalfExtent;
    float shellThickness = max(uShellThickness, 1e-4);
    float atmoBoundR = uAtmoBoundRadius;

    vec2 atmoHit = raySphere(camPos, rayDir, atmoBoundR);
    if (atmoHit.y <= atmoHit.x || atmoHit.y <= 0.0) { discard; }

    float tStart = max(atmoHit.x, 0.0);
    float tEnd = atmoHit.y;

    vec2 groundHit = rayBox(camPos, rayDir, planetHalf);
    bool hitsGround = groundHit.x < groundHit.y && groundHit.x > 0.0;
    if (hitsGround) tEnd = min(tEnd, groundHit.x);

    if (tEnd <= tStart) { discard; }

    float segmentLen = tEnd - tStart;
    float stepSize = segmentLen / float(NUM_VIEW_STEPS);

    vec3 viewDepth = vec3(0.0);
    vec3 inScatterRayleigh = vec3(0.0);
    vec3 inScatterMie = vec3(0.0);
    vec3 ozoneTint = vec3(0.0);

    vec3 rayleighCoeff = max(uColor, vec3(0.02)) * RAYLEIGH_STRENGTH;
    vec3 mieCoeff = vec3(MIE_STRENGTH_BASE * max(uMieStrength, 0.0));

    float cosTheta = dot(rayDir, uSunDir);
    float rayleighPhase = 3.0 / (16.0 * PI) * (1.0 + cosTheta * cosTheta);

    float g = clamp(uMieG, -0.99, 0.99);
    float g2 = g * g;
    float miePhase = (1.0 - g2) / (4.0 * PI * pow(max(1e-4, 1.0 + g2 - 2.0 * g * cosTheta), 1.5));

    for (int i = 0; i < NUM_VIEW_STEPS; i++)
    {
        float t = tStart + stepSize * (float(i) + 0.5);
        vec3 samplePos = camPos + rayDir * t;
        float altitude = sdBox(samplePos, planetHalf);

        float densR = densityAt(altitude, shellThickness, uRayleighScaleHeight);
        float densM = densityAt(altitude, shellThickness, uMieScaleHeight);
        vec3 ozone = ozoneAbsorption(altitude, shellThickness) * uOzoneStrength;

        viewDepth.x += densR * stepSize;
        viewDepth.y += densM * stepSize;
        viewDepth.z += ozone.g * stepSize;
        vec3 sunDepth = sunOpticalDepth(samplePos, uSunDir, planetHalf, atmoBoundR, shellThickness, uRayleighScaleHeight, uMieScaleHeight);

        vec3 totalRayleighDepth = (viewDepth.x + sunDepth.x) * rayleighCoeff;
        vec3 totalMieDepth = vec3((viewDepth.y + sunDepth.y) * mieCoeff.x * 1.1);
        vec3 totalOzoneDepth = (viewDepth.z + sunDepth.z) * ozone * 6.0;
        vec3 transmittance = exp(-(totalRayleighDepth + totalMieDepth + totalOzoneDepth));

        inScatterRayleigh += densR * transmittance * stepSize;
        inScatterMie += densM * transmittance * stepSize;
        ozoneTint += ozone * transmittance * stepSize;
    }

    vec3 single = uSunIntensity * (rayleighCoeff * rayleighPhase * inScatterRayleigh + mieCoeff * miePhase * inScatterMie);
    vec3 multi = uMultiScatterStrength * uSunIntensity * rayleighCoeff * (inScatterRayleigh + inScatterMie) * (1.0 / (4.0 * PI));

    vec3 col = single + multi;
    col -= ozoneTint * 0.4;

    float ndotv = 1.0 - clamp01(dot(normalize(vObjPos), uCamDir));
    col += uColor * pow(ndotv, max(uFresnelPower, 0.5)) * 0.15;

    col *= uOpacity;

    float extinction = 1.0 - exp(-(viewDepth.x * 2.2 + viewDepth.y * 1.4));
    float alpha = clamp01(extinction * uOpacity + luminance(col) * 0.05);

    vec3 tonemapped = col / (1.0 + col * 0.6);
    fragColor = vec4(clamp(tonemapped, 0.0, 1.0), alpha);
}
