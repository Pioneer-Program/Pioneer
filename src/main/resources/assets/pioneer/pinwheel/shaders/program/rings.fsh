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

uniform float uShadowFloor;
uniform float uShadowSoft;
uniform float uForwardScatter;
uniform float uExposure;

out vec4 fragColor;

const float PI = 3.14159265359;

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
    vec2 h = rayBox(p, uSunDir, uPlanetHalf);
    if (h.x < h.y && h.y > 0.0) return 0.0;

    float tc = max(-dot(p, uSunDir), 0.0);
    float soft = max(uShadowSoft, 1e-4);

    float d = 1e9;
    for (int i = -2; i <= 2; i++)
    {
        float t = max(tc + float(i) * soft * 2.0, 0.0);
        d = min(d, sdBox(p + uSunDir * t, uPlanetHalf));
    }

    return smoothstep(0.0, soft, d);
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
    float dens = bandProfile(u);

    float tau = dens * uOpacity * 3.0 / max(cosTilt, 0.02);
    float alpha = (1.0 - exp(-tau)) * cover;
    if (alpha <= 0.002) discard;

    vec3 albedo = mix(uColorMin, uColorMax, vnoise(u * uBandScale * 0.5 + 5.0));

    float lit = sunVisibility(p);
    lit = uShadowFloor + (1.0 - uShadowFloor) * lit;

    float cosSun = dot(rd, uSunDir);
    float fwd = pow(clamp(cosSun, 0.0, 1.0), 8.0);
    float back = pow(clamp(-cosSun, 0.0, 1.0), 3.0) * 0.25;
    float phase = 1.0 + uForwardScatter * (fwd * 4.0 + back);

    float translucency = exp(-tau) * fwd * uForwardScatter;

    vec3 col = (albedo * lit * phase + albedo * translucency) * uExposure;
    col = col / (1.0 + col * 0.35);

    fragColor = vec4(col, alpha);
}
