#version 150

in vec3 vObjPos;

uniform sampler2D uFaceFront;
uniform sampler2D uFaceBack;
uniform sampler2D uFaceLeft;
uniform sampler2D uFaceRight;
uniform sampler2D uFaceTop;
uniform sampler2D uFaceBottom;

uniform vec3  uCamPos;
uniform vec3  uSunDir;
uniform float uHalf;
uniform float uAlpha;
uniform float uNightFloor;
uniform float uTerminator;
uniform float uCurvature;
uniform float uScatterWidth;
uniform float uScatterStrength;
uniform vec3  uScatterColor;

uniform float uRingInner;
uniform float uRingOuter;
uniform float uRingSquareness;
uniform float uRingBandScale;
uniform float uRingBandContrast;
uniform float uRingGapStrength;
uniform float uRingOpacity;
uniform float uRingSeed;

out vec4 fragColor;

float hash1(float n) { return fract(sin(n * 127.1 + uRingSeed * 7.13) * 43758.5453123); }

float vnoise(float x)
{
    float i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash1(i), hash1(i + 1.0), f);
}

float bandProfile(float u)
{
    float x = u * uRingBandScale;

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
    gap = mix(1.0, gap, uRingGapStrength);

    float shaped = mix(1.0 - uRingBandContrast, 1.0, d);
    return clamp(shaped * gap, 0.0, 1.0);
}

float ringTransmittance(vec3 p)
{
    if (uRingOuter <= 0.0 || uRingOuter <= uRingInner) return 1.0;

    float sy = uSunDir.y;
    if (abs(sy) < 1e-5) return 1.0;

    float t = -p.y / sy;
    if (t <= 0.0) return 1.0;

    vec3 h = p + uSunDir * t;
    float r = mix(length(h.xz), max(abs(h.x), abs(h.z)), uRingSquareness);
    if (r < uRingInner || r > uRingOuter) return 1.0;

    float u = (r - uRingInner) / max(uRingOuter - uRingInner, 1e-5);
    float tau = bandProfile(u) * uRingOpacity * 3.0 / max(abs(sy), 0.02);

    return exp(-tau);
}

vec2 rayBox(vec3 ro, vec3 rd, float h)
{
    vec3 s = vec3(abs(rd.x) < 1e-6 ? 1e-6 : rd.x,
    abs(rd.y) < 1e-6 ? 1e-6 : rd.y,
    abs(rd.z) < 1e-6 ? 1e-6 : rd.z);
    vec3 inv = 1.0 / s;
    vec3 t0 = (vec3(-h) - ro) * inv;
    vec3 t1 = (vec3( h) - ro) * inv;
    vec3 tn = min(t0, t1), tf = max(t0, t1);
    return vec2(max(max(tn.x, tn.y), tn.z), min(min(tf.x, tf.y), tf.z));
}

void main()
{
    vec3 ro = uCamPos;
    vec3 rd = normalize(vObjPos - ro);

    vec2 hit = rayBox(ro, rd, uHalf);
    if (hit.x > hit.y || hit.y < 0.0) discard;

    float t = max(hit.x, 0.0);
    vec3 surf = ro + rd * t;

    vec3 n = surf / uHalf;
    vec3 a = abs(n);
    n = clamp(n, -1.0, 1.0);

    vec3 nrm;
    vec2 uv;
    vec4 tex;

    if (a.x >= a.y && a.x >= a.z)
    {
        if (n.x > 0.0)
        {
            nrm = vec3(1.0, 0.0, 0.0);
            uv  = vec2(0.5 + n.z * 0.5, 0.5 - n.y * 0.5);
            tex = texture(uFaceRight, uv);
        }
        else
        {
            nrm = vec3(-1.0, 0.0, 0.0);
            uv  = vec2(0.5 - n.z * 0.5, 0.5 - n.y * 0.5);
            tex = texture(uFaceLeft, uv);
        }
    }
    else if (a.y >= a.z)
    {
        if (n.y > 0.0)
        {
            nrm = vec3(0.0, 1.0, 0.0);
            uv  = vec2(0.5 + n.x * 0.5, 0.5 - n.z * 0.5);
            tex = texture(uFaceTop, uv);
        }
        else
        {
            nrm = vec3(0.0, -1.0, 0.0);
            uv  = vec2(0.5 + n.x * 0.5, 0.5 + n.z * 0.5);
            tex = texture(uFaceBottom, uv);
        }
    }
    else
    {
        if (n.z > 0.0)
        {
            nrm = vec3(0.0, 0.0, 1.0);
            uv  = vec2(0.5 - n.x * 0.5, 0.5 - n.y * 0.5);
            tex = texture(uFaceFront, uv);
        }
        else
        {
            nrm = vec3(0.0, 0.0, -1.0);
            uv  = vec2(0.5 + n.x * 0.5, 0.5 - n.y * 0.5);
            tex = texture(uFaceBack, uv);
        }
    }

    vec3 sn = normalize(mix(nrm, normalize(surf), uCurvature));
    float ndots = dot(sn, uSunDir);
    float lit = smoothstep(-uTerminator, uTerminator, ndots);
    lit *= ringTransmittance(surf);

    float shade = uNightFloor + (1.0 - uNightFloor) * lit;

    float band = exp(-pow(ndots / max(uScatterWidth, 1e-3), 2.0));
    vec3 scatter = uScatterColor * (band * uScatterStrength);

    fragColor = vec4(tex.rgb * (shade + scatter), tex.a * uAlpha);
}
