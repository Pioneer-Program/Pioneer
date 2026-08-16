#version 150

in vec3 vObjPos;

uniform sampler2D uNoiseVolumeLUT;
uniform sampler2D uWeatherLUT;

uniform vec3  uColor;
uniform vec3  uCamDir;
uniform vec3  uSunDir;
uniform float uCamDist;

uniform float uPlanetHalfExtent;
uniform float uCloudInner;
uniform float uCloudOuter;
uniform float uAtmoBoundRadius;

uniform float uCoverage;
uniform float uCoverageBias;
uniform float uDensity;
uniform float uErosion;

uniform vec3 uWindOffset;
uniform vec2 uWeatherDriftSC;
uniform float uVolumeFreq;
uniform float uInvShellThickness;
uniform float uExtinction;
uniform float uMaxSegmentLen;
uniform float uSunMarchLen;
uniform float uInvSunSteps;

uniform float uMieG;
uniform float uSunIntensity;
uniform float uMultiScatterStrength;
uniform float uPowderStrength;

uniform int uViewSteps;
uniform int uSunSteps;

out vec4 fragColor;

const int MAX_VIEW_STEPS = 16;
const int MAX_SUN_STEPS  = 3;
const float PI = 3.14159265359;

const float NIGHT_CUTOFF = -0.55;
const float SHADOW_SKIP_DENSITY = 0.02;
const float ALPHA_CUTOFF = 0.99;

const float NV_RES = 64.0;
const float NV_TILES = 8.0;
const float NV_ATLAS = 512.0;
const float NV_TILE_UV = NV_RES / NV_ATLAS;
const float NV_INSET = (NV_RES - 1.0) / NV_ATLAS;
const float NV_HALF = 0.5 / NV_ATLAS;
const float NV_INV_T = 1.0 / NV_TILES;

float clamp01(float v) { return clamp(v, 0.0, 1.0); }

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
    vec3 invDir = 1.0 / mix(dir, vec3(1e-6), lessThan(abs(dir), vec3(1e-6)));
    vec3 t0 = (-vec3(halfExtent) - origin) * invDir;
    vec3 t1 = ( vec3(halfExtent) - origin) * invDir;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    return vec2(max(max(tmin.x, tmin.y), tmin.z), min(min(tmax.x, tmax.y), tmax.z));
}

vec2 tileOffset(float slice)
{
    return vec2(mod(slice, NV_TILES), floor(slice * NV_INV_T)) * NV_TILE_UV;
}

vec4 sampleVolume(vec3 p)
{
    p = fract(p);
    vec2 uv = p.xy * NV_INSET + NV_HALF;

    float z  = p.z * NV_RES;
    float z0 = floor(z);
    float fz = z - z0;

    return mix(texture(uNoiseVolumeLUT, uv + tileOffset(mod(z0, NV_RES))), texture(uNoiseVolumeLUT, uv + tileOffset(mod(z0 + 1.0, NV_RES))), fz);
}

vec4 sampleVolumeCoarse(vec3 p)
{
    p = fract(p);
    vec2 uv = p.xy * NV_INSET + NV_HALF;
    float slice = mod(floor(p.z * NV_RES + 0.5), NV_RES);
    return texture(uNoiseVolumeLUT, uv + tileOffset(slice));
}

vec2 octaEncode(vec3 n)
{
    n *= 1.0 / (abs(n.x) + abs(n.y) + abs(n.z) + 1e-6);
    vec2 uv = n.xz;
    if (n.y < 0.0)
    uv = (1.0 - abs(uv.yx)) * vec2(uv.x >= 0.0 ? 1.0 : -1.0, uv.y >= 0.0 ? 1.0 : -1.0);
    return uv * 0.5 + 0.5;
}

vec2 sampleWeather(vec3 dirN)
{
    vec3 d = vec3(dirN.x * uWeatherDriftSC.y - dirN.z * uWeatherDriftSC.x, dirN.y, dirN.x * uWeatherDriftSC.x + dirN.z * uWeatherDriftSC.y);
    return texture(uWeatherLUT, octaEncode(d)).rg;
}

float shapeDensity(vec4 n, float coverage, float cloudType, float alt01)
{
    float bottomFade = smoothstep(0.0, mix(0.35, 0.08, cloudType), alt01);
    float topFade = smoothstep(1.0, mix(0.55, 0.85, cloudType), alt01);

    float shaped = clamp01((n.r - (1.0 - coverage)) / max(coverage, 0.05)) * bottomFade * topFade;
    if (shaped <= 0.001) return 0.0;

    float erosionNoise = n.b * 0.5;
    return clamp01(shaped - erosionNoise * uErosion * (1.0 - shaped * 0.5)) * uDensity;
}

float henyeyGreenstein(float cosTheta, float g)
{
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * PI * pow(max(1e-4, 1.0 + g2 - 2.0 * g * cosTheta), 1.5));
}

float phaseCloud(float cosTheta, float g)
{
    return mix(henyeyGreenstein(cosTheta, -g * 0.25), henyeyGreenstein(cosTheta, g), 0.7);
}

float sunShadowDensity(vec3 origin, float cloudInner, float cloudOuter, float planetHalf, float coverage, float cloudType)
{
    float stepSize = uSunMarchLen * uInvSunSteps;
    float accum = 0.0;

    for (int i = 0; i < MAX_SUN_STEPS; i++)
    {
        if (i >= uSunSteps) break;

        vec3 samplePos = origin + uSunDir * (stepSize * (float(i) + 0.5));
        float alt = sdBox(samplePos, planetHalf);
        if (alt < cloudInner - 0.02 || alt > cloudOuter + 0.02) continue;

        float alt01 = clamp01((alt - cloudInner) * uInvShellThickness);
        accum += shapeDensity(sampleVolumeCoarse(samplePos * uVolumeFreq + uWindOffset), coverage, cloudType, alt01) * stepSize;

        if (accum > 1.2) break;
    }
    return accum;
}

float ambientFill(float altitude01) { return mix(0.55, 1.15, altitude01); }

float dither(vec2 fragCoord)
{
    return fract(52.9829189 * fract(dot(fragCoord, vec2(0.06711056, 0.00583715))));
}

void main()
{
    vec3 camPos = uCamDir * uCamDist;
    vec3 rayDir = normalize(vObjPos - camPos);

    float planetHalf = uPlanetHalfExtent;
    float cloudInner = max(uCloudInner, 1e-4);
    float cloudOuter = max(uCloudOuter, cloudInner + 1e-4);

    vec2 atmoHit = raySphere(camPos, rayDir, uAtmoBoundRadius);
    if (atmoHit.y <= atmoHit.x || atmoHit.y <= 0.0) { discard; }

    vec2 outerBoxHit = rayBox(camPos, rayDir, planetHalf + cloudOuter);
    if (outerBoxHit.x >= outerBoxHit.y || outerBoxHit.y <= 0.0) { discard; }

    vec2 groundHit = rayBox(camPos, rayDir, planetHalf);
    if (groundHit.x < 0.0 && groundHit.y > 0.0) { discard; }

    vec2 innerBoxHit = rayBox(camPos, rayDir, planetHalf + cloudInner);
    bool camInsideOuter = outerBoxHit.x < 0.0 && outerBoxHit.y > 0.0;

    float tStart = max(outerBoxHit.x, 0.0);
    float tEnd = outerBoxHit.y;

    bool hitsInner = camInsideOuter ? (innerBoxHit.y > tStart && innerBoxHit.x < 0.0) : (innerBoxHit.x < innerBoxHit.y && innerBoxHit.x > tStart);
    if (hitsInner) tEnd = min(tEnd, camInsideOuter ? innerBoxHit.y : innerBoxHit.x);
    if (groundHit.y > tStart && groundHit.x > 0.0) tEnd = min(tEnd, groundHit.x);

    if (tEnd <= tStart) { discard; }

    float stepSize = (min(tEnd, tStart + uMaxSegmentLen) - tStart) / float(uViewSteps);

    float phase = phaseCloud(dot(rayDir, uSunDir), clamp(uMieG, 0.0, 0.99));

    const vec3 warmTint = vec3(1.08, 1.00, 0.92);
    const vec3 coolShadowTint = vec3(0.72, 0.78, 0.92);
    const vec3 duskTint = vec3(1.25, 0.62, 0.28);

    vec3  accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    float t = tStart + stepSize * dither(gl_FragCoord.xy);

    for (int i = 0; i < MAX_VIEW_STEPS; i++)
    {
        if (i >= uViewSteps || accumAlpha > ALPHA_CUTOFF) break;

        vec3 samplePos = camPos + rayDir * t;
        t += stepSize;

        vec3 nrm = samplePos * inversesqrt(dot(samplePos, samplePos));

        float sunFacing = dot(nrm, uSunDir);
        float dayAmt = smoothstep(NIGHT_CUTOFF, 0.5, sunFacing);

        if (dayAmt <= 0.001) continue;

        vec2  weather  = sampleWeather(nrm);
        float coverage = clamp01(weather.x + uCoverageBias);
        if (coverage <= 0.001) continue;

        float alt = sdBox(samplePos, planetHalf);
        float alt01 = clamp01((alt - cloudInner) * uInvShellThickness);

        float density = shapeDensity(sampleVolume(samplePos * uVolumeFreq + uWindOffset), coverage, weather.y, alt01);
        if (density <= 0.001) continue;

        float directLight = 1.0;
        if (density > SHADOW_SKIP_DENSITY)
        {
            float shadowDepth = sunShadowDensity(samplePos, cloudInner, cloudOuter, planetHalf, coverage, weather.y);
            directLight = exp(-shadowDepth * 5.0);
        }

        float duskAmt = 1.0 - abs(clamp(sunFacing, -0.4, 0.4)) * 2.5;
        float powder  = 1.0 - exp(-density * 2.0 * uPowderStrength);

        vec3  litTint = mix(coolShadowTint, warmTint, clamp01(directLight));
        float ambient = ambientFill(alt01) * uMultiScatterStrength;
        vec3 litColor = litTint * (uSunIntensity * directLight * 1.75 * phase * mix(1.0, powder, 0.6) + ambient * 0.55 + directLight * uSunIntensity * 0.18);

        litColor = mix(litColor, litColor * duskTint, duskAmt * 0.85) * dayAmt;

        float sampleAlpha = clamp01(1.0 - exp(-density * stepSize * uExtinction));
        float weight = sampleAlpha * (1.0 - accumAlpha);
        accumColor += litColor * weight;
        accumAlpha += weight;
    }

    if (accumAlpha <= 0.003) { discard; }

    vec3 tonemapped = accumColor * uColor / (1.0 + accumColor * 0.5);
    fragColor = vec4(clamp(tonemapped, 0.0, 1.0), clamp01(accumAlpha));
}
