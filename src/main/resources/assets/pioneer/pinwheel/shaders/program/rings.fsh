#version 150

in vec3 vObjPos;

uniform vec3 uCamPos;
uniform vec3 uSunDir;

uniform float uPlanetHalf;
uniform float uInnerR;
uniform float uOuterR;
uniform float uSquareness;

uniform vec3  uColorMin;
uniform vec3  uColorMax;
uniform float uOpacity;
uniform float uBandScale;
uniform float uBandContrast;
uniform float uGapStrength;
uniform float uSeed;

uniform float uSunAngRad;
uniform float uSSA;
uniform float uAsymmetry;
uniform float uBackFrac;
uniform float uOppB0;
uniform float uOppH;
uniform float uPlanetShine;
uniform vec3  uPlanetTint;
uniform float uExposure;
uniform int   uDebug;

out vec4 fragColor;

const float PI = 3.14159265359;

const float RADIANCE_NORM = 8.0;

float hash1(float n)
{
    return fract(sin(n * 127.1 + uSeed * 7.13) * 43758.5453123);
}

float vnoise(float x)
{
    float i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash1(i), hash1(i + 1.0), f);
}

float bandProfile(float u)
{
    float x = u * uBandScale;

    float d = 0.0, amp = 0.5, freq = 1.0;
    for (int i = 0; i < 5; i++)
    {
        d += amp * vnoise(x * freq + float(i) * 19.7);
        freq *= 2.03;
        amp *= 0.55;
    }
    d /= 0.94;

    float gap = 1.0;
    for (int i = 0; i < 3; i++)
    {
        float c = 0.15 + 0.7 * hash1(float(i) * 3.3 + 11.0);
        float w = 0.012 + 0.03 * hash1(float(i) * 5.1 + 23.0);
        gap *= smoothstep(0.0, 1.0, abs(u - c) / w);
    }
    gap = mix(1.0, gap, uGapStrength);

    float shaped = mix(1.0 - uBandContrast, 1.0, d);
    return clamp(shaped * gap, 0.0, 1.0);
}

vec2 rayBox(vec3 ro, vec3 rd, float h)
{
    vec3 s = vec3(abs(rd.x) < 1e-6 ? 1e-6 : rd.x, abs(rd.y) < 1e-6 ? 1e-6 : rd.y, abs(rd.z) < 1e-6 ? 1e-6 : rd.z);
    vec3 inv = 1.0 / s;
    vec3 t0 = (vec3(-h) - ro) * inv;
    vec3 t1 = (vec3( h) - ro) * inv;
    vec3 tn = min(t0, t1), tf = max(t0, t1);
    return vec2(max(max(tn.x, tn.y), tn.z), min(min(tf.x, tf.y), tf.z));
}

float sdBox(vec3 p, float h)
{
    vec3 q = abs(p) - vec3(h);
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

float sunVisibility(vec3 p)
{
    float tc  = max(-dot(p, uSunDir), 0.0);
    float d   = sdBox(p + uSunDir * tc, uPlanetHalf);
    float pen = max(tc * uSunAngRad, 1e-5);
    return smoothstep(-pen, pen, d);
}

float hg(float cosT, float g)
{
    float g2 = g * g;
    float d = 1.0 + g2 - 2.0 * g * cosT;
    return (1.0 - g2) / (d * sqrt(max(d, 1e-6)));
}

float phaseFn(float cosT)
{
    return mix(hg(cosT, uAsymmetry), hg(cosT, -uAsymmetry * 0.6), uBackFrac);
}

float oppositionSurge(float cosPhase)
{
    float c = clamp(cosPhase, -1.0, 1.0);
    float sinP = sqrt(max(1.0 - c * c, 0.0));
    float tanHalf = sinP / max(1.0 + c, 1e-4);
    return 1.0 + uOppB0 / (1.0 + tanHalf / max(uOppH, 1e-4));
}

void main()
{
    vec3 ro = uCamPos;
    vec3 rd = normalize(vObjPos - ro);

    float cosTilt = abs(rd.y);
    if (cosTilt < 1e-5) discard;

    float t = -ro.y / rd.y;
    if (t <= 0.0) discard;

    vec3 p = ro + rd * t;
    float r = mix(length(p.xz), max(abs(p.x), abs(p.z)), uSquareness);
    float rw = max(fwidth(r), 1e-5);
    float cover = smoothstep(uInnerR - rw, uInnerR + rw, r) * (1.0 - smoothstep(uOuterR - rw, uOuterR + rw, r));
    if (cover <= 0.001) discard;

    vec2 bh = rayBox(ro, rd, uPlanetHalf);
    if (bh.x < bh.y && bh.x > 0.0 && bh.x < t) discard;

    float u = (r - uInnerR) / max(uOuterR - uInnerR, 1e-5);

    float tau = bandProfile(u) * uOpacity * 3.0;

    float muV = max(cosTilt, 0.02);
    float mu0 = max(abs(uSunDir.y), 0.02);

    float Tview = exp(-tau / muV);
    float alpha = (1.0 - Tview) * cover;
    if (alpha <= 0.002) discard;

    vec3 albedo = mix(uColorMin, uColorMax, vnoise(u * uBandScale * 0.5 + 5.0));

    float lit = sunVisibility(p);

    float cosT = dot(rd, uSunDir);
    float p0 = phaseFn(cosT);

    float k = 0.25 * uSSA * p0 * lit;
    float I;

    if ((-rd.y) * uSunDir.y > 0.0)
    {
        I = k * (mu0 / (mu0 + muV)) * (1.0 - exp(-tau * (1.0 / muV + 1.0 / mu0)));
        I *= oppositionSurge(-cosT);
    }
    else
    {
        float dm = mu0 - muV;
        I = (abs(dm) < 1e-3)
            ? k * (tau / muV) * exp(-tau / muV)
            : k * (mu0 / dm) * (exp(-tau / mu0) - exp(-tau / muV));
    }

    float solid = uPlanetHalf * uPlanetHalf / max(dot(p, p), 1e-4);
    float face  = 0.5 + 0.5 * dot(normalize(-p), uSunDir);
    vec3  shine = uPlanetTint * (0.25 * uSSA * uPlanetShine * solid * face * (1.0 - Tview));

    if (uDebug != 0)
    {
        float tc = max(-dot(p, uSunDir), 0.0);
        bool sameSide = (-rd.y) * uSunDir.y > 0.0;

        if (uDebug == 1) fragColor = vec4(1.0 - lit, lit, 0.0, 1.0);
        else if (uDebug == 2) fragColor = vec4(u, fract(r / max(uPlanetHalf, 1e-4)), clamp(tc / max(uOuterR, 1e-4), 0.0, 1.0), 1.0);
        else if (uDebug == 3) fragColor = vec4(mu0, muV, clamp(tau, 0.0, 1.0), 1.0);
        else if (uDebug == 4) fragColor = vec4(clamp(p0 * 0.25, 0.0, 1.0), clamp(oppositionSurge(-cosT) - 1.0, 0.0, 1.0), 0.5 + 0.5 * cosT, 1.0);
        else if (uDebug == 5) fragColor = vec4(sameSide ? 1.0 : 0.0, sameSide ? 0.0 : 1.0, clamp(I * 8.0, 0.0, 1.0), 1.0);
        else if (uDebug == 6) fragColor = vec4(clamp(tc * uSunAngRad / max(uPlanetHalf, 1e-4) * 20.0, 0.0, 1.0), clamp(tc / max(uOuterR, 1e-4), 0.0, 1.0), 0.0, 1.0);
        else fragColor = vec4(rd.y < 0.0 ? 1.0 : 0.0, uSunDir.y > 0.0 ? 1.0 : 0.0, 0.5, 1.0);
        return;
    }

    vec3 radiance = (albedo * I + albedo * shine) * (uExposure * RADIANCE_NORM);
    vec3 col = radiance / max(alpha, 1e-4);
    col = col / (1.0 + col * 0.35);

    fragColor = vec4(col, alpha);
}
