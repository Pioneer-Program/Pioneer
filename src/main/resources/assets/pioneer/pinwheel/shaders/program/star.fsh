#version 150

in vec3 vObjPos;

uniform vec3  uCamPos;
uniform float uCoreRadius;
uniform float uGlowRadius;
uniform vec3  uCoreColor;
uniform vec3  uGlowColor;
uniform float uIntensity;
uniform float uFalloff;
uniform float uGranulation;
uniform float uSquareness;
uniform float uWhiteHot;
uniform float uChromosphere;
uniform float uProminence;
uniform float uRayStrength;
uniform float uSpin;
uniform float uTime;

const int STEPS = 16;

out vec4 fragColor;

float hash13(vec3 p)
{
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec3 p)
{
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    return mix(mix(mix(hash13(i + vec3(0,0,0)), hash13(i + vec3(1,0,0)), f.x), mix(hash13(i + vec3(0,1,0)), hash13(i + vec3(1,1,0)), f.x), f.y), mix(mix(hash13(i + vec3(0,0,1)), hash13(i + vec3(1,0,1)), f.x), mix(hash13(i + vec3(0,1,1)), hash13(i + vec3(1,1,1)), f.x), f.y), f.z);
}

float fbm(vec3 p)
{
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++)
    {
        v += a * vnoise(p);
        p *= 2.07;
        a *= 0.5;
    }
    return v / 0.875;
}

float ridged(vec3 p)
{
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++)
    {
        v += a * (1.0 - abs(vnoise(p) * 2.0 - 1.0));
        p *= 2.11;
        a *= 0.5;
    }
    return v / 0.875;
}

vec3 spin(vec3 p)
{
    float c = cos(uSpin), s = sin(uSpin);
    return vec3(c * p.x - s * p.z, p.y, s * p.x + c * p.z);
}

float shapeDist(vec3 p)
{
    return mix(length(p), max(abs(p.x), max(abs(p.y), abs(p.z))), uSquareness);
}

vec3 shapeNormal(vec3 p)
{
    vec3 a = abs(p);
    vec3 boxN = vec3(a.x >= a.y && a.x >= a.z ? sign(p.x) : 0.0,
    a.y > a.x && a.y >= a.z ? sign(p.y) : 0.0,
    a.z > a.x && a.z > a.y ? sign(p.z) : 0.0);
    return normalize(mix(normalize(p), boxN, uSquareness));
}

vec2 raySphere(vec3 ro, vec3 rd, float r)
{
    float b = dot(ro, rd);
    float c = dot(ro, ro) - r * r;
    float h = b * b - c;
    if (h < 0.0) return vec2(1.0, -1.0);
    h = sqrt(h);
    return vec2(-b - h, -b + h);
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

vec3 heat(float t, vec3 tint)
{
    t = clamp(t, 0.0, 1.0);
    vec3 deep = tint * 0.35;
    vec3 mid = tint;
    vec3 hot = mix(tint, vec3(1.0), uWhiteHot);

    return t < 0.5 ? mix(deep, mid, t * 2.0)
    : mix(mid, hot, (t - 0.5) * 2.0);
}

void main()
{
    vec3 ro = uCamPos;
    vec3 rd = normalize(vObjPos - ro);

    vec2 outer = rayBox(ro, rd, uGlowRadius);
    if (outer.x > outer.y || outer.y < 0.0) discard;

    vec2 sph = raySphere(ro, rd, uCoreRadius);
    vec2 box = rayBox(ro, rd, uCoreRadius);
    vec2 core = uSquareness >= 0.999 ? box : (uSquareness <= 0.001 ? sph : mix(sph, box, uSquareness));
    bool hitsCore = core.x <= core.y && core.y > 0.0;

    float tStart = max(outer.x, 0.0);
    float tEnd = hitsCore ? max(core.x, tStart) : outer.y;

    float jitter = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    float dt = (tEnd - tStart) / float(STEPS);

    vec3 mid = ro + rd * mix(tStart, tEnd, 0.35);
    vec3 dir = normalize(mid);
    vec3 sd = spin(dir * 2.6);

    float rays = pow(clamp(ridged(sd + vec3(0.0, uTime * 0.02, 0.0)), 0.0, 1.0), 2.6);
    float streamer = mix(1.0, 0.30 + 2.0 * rays, uRayStrength);

    float turb = mix(0.7, 1.35, fbm(sd * 1.9 + uTime * 0.06));
    float tongue = pow(clamp(ridged(spin(dir * 7.0) + uTime * 0.09), 0.0, 1.0), 3.5);

    float angular = turb * streamer;

    vec3 glow = vec3(0.0);

    for (int i = 0; i < STEPS; i++)
    {
        vec3 p = ro + rd * (tStart + (float(i) + jitter) * dt);
        float d = shapeDist(p);

        float h = clamp((d - uCoreRadius) / max(uGlowRadius - uCoreRadius, 1e-4), 0.0, 1.0);

        float inner = pow(1.0 - h, uFalloff);
        float outerHalo = pow(1.0 - h, 1.5) * 0.10;
        float rim = exp(-h * 16.0) * uChromosphere;
        float prom = tongue * exp(-h * 5.0) * uProminence;
        float dens = inner * angular + outerHalo + rim + prom;

        glow += heat(0.35 + 0.5 * (1.0 - h), uGlowColor) * dens;
    }

    glow *= dt / max(uGlowRadius, 1e-4);

    vec3 col = glow * uIntensity;
    float alpha = clamp(max(max(col.r, col.g), col.b), 0.0, 1.0);

    if (hitsCore)
    {
        vec3 p = ro + rd * max(core.x, 0.0);
        vec3 n = shapeNormal(p);
        vec3 dir = normalize(p);

        float mu = clamp(dot(n, -rd), 0.0, 1.0);
        float limb = 0.30 + 0.70 * pow(mu, 0.8);

        vec3 q = spin(dir * 5.0) + vec3(0.0, uTime * 0.04, 0.0);
        float supergran = fbm(q * 0.9);
        float cells = ridged(q * 3.6 + supergran * 1.5);
        float grain = mix(supergran, cells, 0.7);
        float spots = smoothstep(0.62, 0.78, fbm(spin(dir * 2.2) + 40.0));

        float temp = limb * mix(1.0 - uGranulation, 1.0 + uGranulation, grain);
        temp *= 1.0 - spots * 0.75;

        col += heat(temp, uCoreColor) * uIntensity;
        alpha = 1.0;
    }

    col = col / (1.0 + col * 0.55);
    col = pow(col, vec3(0.85));

    fragColor = vec4(col, alpha);
}
