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
// uAtmoBoundRadius

uniform float uCoverageBias;
uniform float uDensity;
uniform float uErosion;

uniform vec3 uVolumeX;
uniform vec3 uVolumeY;
uniform vec3 uVolumeZ;
uniform vec3 uWindOffset;
uniform vec3 uDetailOffset;
uniform float uDetailRatio;
uniform float uDetile;
uniform vec2 uWeatherDriftSC;
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
const float SHAPE_SLOPE = 1.6;
const float TAPER_FLAT = 0.30;
const float TAPER_TALL = 0.85;
const float TAPER_PIVOT = 0.4;
const float COVERAGE_RANGE = 1.4;
const float EROSION_GAIN = 1.5;
const float EROSION_CORE = 0.75;
const float EROSION_MAX = 0.95;
const float SHADOW_EXTINCTION = 3.5;
const float SHADOW_SCATTER = 0.7;

const float NV_RES = 64.0;
const float NV_TILES = 8.0;
const float NV_TILE = NV_RES + 1.0;
const float NV_INV_ATLAS = 1.0 / (NV_TILE * NV_TILES);
const float NV_INV_T = 1.0 / NV_TILES;

float clamp01(float v) { return clamp(v, 0.0, 1.0); }

float sdBox(vec3 p, float halfExtent)
{
    vec3 q = abs(p) - vec3(halfExtent);
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

vec2 rayBox(vec3 invDir, vec3 oi, float halfExtent)
{
    vec3 k = abs(invDir) * halfExtent;
    vec3 t1 = -oi - k;
    vec3 t2 = -oi + k;
    return vec2(max(max(t1.x, t1.y), t1.z), min(min(t2.x, t2.y), t2.z));
}

vec2 tileOffset(float slice)
{
    return vec2(mod(slice, NV_TILES), floor(slice * NV_INV_T)) * NV_TILE;
}

vec3 toVolume(vec3 p)
{
    return vec3(dot(uVolumeX, p), dot(uVolumeY, p), dot(uVolumeZ, p));
}

vec4 fetchVolume(vec3 p, out float fz)
{
    p = fract(p);
    float z  = p.z * NV_RES;
    float z0 = min(floor(z), NV_RES - 1.0);
    fz = z - z0;
    vec2 texel = tileOffset(z0) + p.xy * NV_RES + 0.5;
    return textureLod(uNoiseVolumeLUT, texel * NV_INV_ATLAS, 0.0);
}

float sampleShape(vec3 p)
{
    float fz;
    vec4 n = fetchVolume(p, fz);
    return mix(n.r, n.g, fz);
}

float sampleDetail(vec3 p)
{
    float fz;
    vec4 n = fetchVolume(p, fz);
    return mix(n.b, n.a, fz);
}

vec2 octaEncode(vec3 n)
{
    n *= 1.0 / (abs(n.x) + abs(n.y) + abs(n.z) + 1e-6);
    vec2 uv = n.xz;
    if (n.y < 0.0)
    uv = (1.0 - abs(uv.yx)) * vec2(uv.x >= 0.0 ? 1.0 : -1.0, uv.y >= 0.0 ? 1.0 : -1.0);
    return uv * 0.5 + 0.5;
}

vec4 sampleWeather(vec3 dirN)
{
    vec3 d = vec3(dirN.x * uWeatherDriftSC.y - dirN.z * uWeatherDriftSC.x, dirN.y, dirN.x * uWeatherDriftSC.x + dirN.z * uWeatherDriftSC.y);
    return textureLod(uWeatherLUT, octaEncode(d), 0.0);
}

float shapeFade(float alt01, float bottomEdge, float topEdge)
{
    return smoothstep(0.0, bottomEdge, alt01) * (1.0 - smoothstep(topEdge, 1.0, alt01));
}

float erode(float shaped, float detail)
{
    float e = min(detail * uErosion * (EROSION_GAIN - shaped * EROSION_CORE), EROSION_MAX);
    return clamp01((shaped - e) / (1.0 - e)) * uDensity;
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

float sunShadowDensity(vec3 origin, vec3 volumePos, vec3 sunStep, vec3 sunStepVolume, float cloudInner, float cloudOuter, float planetHalf, float coverage, float bottomEdge, float topEdge, float taperScale)
{
    float weight = uDensity * uSunMarchLen * uInvSunSteps;
    float accum = 0.0;

    for (int i = 0; i < MAX_SUN_STEPS; i++)
    {
        if (i >= uSunSteps) break;

        float s = float(i) + 0.5;
        float alt = sdBox(origin + sunStep * s, planetHalf);
        if (alt <= cloudInner || alt >= cloudOuter) continue;

        float alt01 = clamp01((alt - cloudInner) * uInvShellThickness);
        float covEff = coverage * (1.0 - (alt01 - TAPER_PIVOT) * taperScale);
        float fade = shapeFade(alt01, bottomEdge, topEdge);
        if (clamp01(covEff * SHAPE_SLOPE) * fade <= 0.0) continue;

        float shape = sampleShape(volumePos + sunStepVolume * s);
        accum += clamp01((shape - 1.0 + covEff) * SHAPE_SLOPE) * fade * weight;

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

    vec3 invDir = 1.0 / mix(rayDir, vec3(1e-6), lessThan(abs(rayDir), vec3(1e-6)));
    vec3 oi = camPos * invDir;

    vec2 outerBoxHit = rayBox(invDir, oi, planetHalf + cloudOuter);
    if (outerBoxHit.x >= outerBoxHit.y || outerBoxHit.y <= 0.0) { discard; }

    vec2 groundHit = rayBox(invDir, oi, planetHalf);
    if (groundHit.x < 0.0 && groundHit.y > 0.0) { discard; }

    vec2 innerBoxHit = rayBox(invDir, oi, planetHalf + cloudInner);

    float tStart = max(outerBoxHit.x, 0.0);
    float tEnd = outerBoxHit.y;
    bool innerHit = innerBoxHit.x < innerBoxHit.y && innerBoxHit.y > 0.0;
    bool groundAhead = groundHit.x < groundHit.y && groundHit.x > 0.0;

    if (innerHit)
    {
        if (innerBoxHit.x <= 0.0)
        {
            if (groundAhead) { discard; }
            tStart = max(tStart, innerBoxHit.y);
        }
        else
        {
            tEnd = min(tEnd, innerBoxHit.x);
        }
    }
    if (groundAhead) tEnd = min(tEnd, groundHit.x);

    if (tEnd <= tStart) { discard; }

    float stepSize = (min(tEnd, tStart + uMaxSegmentLen) - tStart) / float(uViewSteps);
    float sigma = stepSize * uExtinction;

    vec3 sunStep = uSunDir * (uSunMarchLen * uInvSunSteps);
    vec3 sunStepVolume = toVolume(sunStep);
    vec3 volumeOrigin = toVolume(camPos);
    vec3 volumeDir = toVolume(rayDir);

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

        float ts = t;
        vec3 samplePos = camPos + rayDir * ts;
        t += stepSize;

        vec3 nrm = samplePos * inversesqrt(dot(samplePos, samplePos));

        float sunFacing = dot(nrm, uSunDir);
        float dayAmt = smoothstep(NIGHT_CUTOFF, 0.5, sunFacing);

        if (dayAmt <= 0.001) continue;

        float alt = sdBox(samplePos, planetHalf);
        if (alt <= cloudInner || alt >= cloudOuter) continue;
        float alt01 = clamp01((alt - cloudInner) * uInvShellThickness);

        vec4 weather = sampleWeather(nrm);
        float coverage = weather.x * COVERAGE_RANGE + uCoverageBias;
        if (coverage <= 0.001) continue;

        float bottomEdge = mix(0.35, 0.08, weather.y);
        float topEdge = mix(0.55, 0.85, weather.y);
        float taperScale = mix(TAPER_FLAT, TAPER_TALL, weather.y);
        float fade = shapeFade(alt01, bottomEdge, topEdge);
        float covEff = coverage * (1.0 - (alt01 - TAPER_PIVOT) * taperScale);
        if (clamp01(covEff * SHAPE_SLOPE) * fade <= 0.001) continue;

        vec3 lattice = volumeOrigin + volumeDir * ts + vec3(weather.zw, weather.z - weather.w) * uDetile;
        vec3 volumePos = lattice + uWindOffset;

        float shaped = clamp01((sampleShape(volumePos) - 1.0 + covEff) * SHAPE_SLOPE) * fade;
        if (shaped <= 0.001) continue;

        float density = erode(shaped, sampleDetail(lattice * uDetailRatio + uDetailOffset));
        if (density <= 0.001) continue;

        float directLight = 1.0;
        if (density > SHADOW_SKIP_DENSITY)
        {
            float shadowDepth = sunShadowDensity(samplePos, volumePos, sunStep, sunStepVolume, cloudInner, cloudOuter, planetHalf, coverage, bottomEdge, topEdge, taperScale);
            float soft = exp(-shadowDepth * SHADOW_EXTINCTION);
            float hard = soft * soft;
            directLight = max(hard * hard, SHADOW_SCATTER * soft);
        }

        float duskAmt = 1.0 - abs(clamp(sunFacing, -0.4, 0.4)) * 2.5;
        float powder  = 1.0 - exp(-density * 2.0 * uPowderStrength);

        vec3  litTint = mix(coolShadowTint, warmTint, clamp01(directLight));
        float ambient = ambientFill(alt01) * uMultiScatterStrength;
        vec3 litColor = litTint * (uSunIntensity * directLight * 1.75 * phase * mix(1.0, powder, 0.6) + ambient * 0.55 + directLight * uSunIntensity * 0.18);

        litColor = mix(litColor, litColor * duskTint, duskAmt * 0.85) * dayAmt;

        float sampleAlpha = clamp01(1.0 - exp(-density * sigma));
        float weight = sampleAlpha * (1.0 - accumAlpha);
        accumColor += litColor * weight;
        accumAlpha += weight;
    }

    if (accumAlpha <= 0.003) { discard; }

    vec3 radiance = accumColor / accumAlpha;
    vec3 tonemapped = radiance * uColor / (1.0 + radiance * 0.5);
    fragColor = vec4(clamp(tonemapped, 0.0, 1.0), clamp01(accumAlpha));
}
