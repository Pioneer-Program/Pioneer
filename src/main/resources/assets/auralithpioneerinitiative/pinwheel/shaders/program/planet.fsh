#version 150

in vec3 vObjPos;
in vec3 vObjNormal;
in vec2 vUv;
in vec4 vColor;

uniform sampler2D uFaceTex;

uniform vec3  uSunDir;
uniform float uAlpha;

uniform float uTerminatorSoftness;
uniform float uNightFloor;

uniform float uRingInnerR;
uniform float uRingOuterR;
uniform float uRingShadowSoftFrac;

uniform sampler2D uWeatherLUT;
uniform float uCloudShadowStrength;
uniform float uCoverageBias;
uniform vec2  uWeatherDriftSC;
uniform float uCloudInner;

out vec4 fragColor;

float clamp01(float v)
{
    return clamp(v, 0.0, 1.0);
}

// TODO: Share this between planet and clouds
vec2 octaEncode(vec3 n)
{
    n *= 1.0 / (abs(n.x) + abs(n.y) + abs(n.z) + 1e-6);
    vec2 uv = n.xz;
    if (n.y < 0.0)
        uv = (1.0 - abs(uv.yx)) * vec2(uv.x >= 0.0 ? 1.0 : -1.0, uv.y >= 0.0 ? 1.0 : -1.0);
    return uv * 0.5 + 0.5;
}

vec2 sampleWeather(vec3 nrm)
{
    vec2 uv = octaEncode(nrm);
    vec2 c = uv - 0.5;
    uv = vec2(c.x * uWeatherDriftSC.y - c.y * uWeatherDriftSC.x, c.x * uWeatherDriftSC.x + c.y * uWeatherDriftSC.y) + 0.5;
    return texture(uWeatherLUT, uv).rg;
}

float ringShadowLit(vec3 p)
{
    if (uRingInnerR <= 0.0 || uRingOuterR <= uRingInnerR) return 1.0;
    if (abs(uSunDir.y) < 1e-6) return 1.0;

    float t = -p.y / uSunDir.y;
    if (t <= 0.0) return 1.0;

    vec2 hit = p.xz + t * uSunDir.xz;
    float radial = length(hit);

    float soft = max((uRingOuterR - uRingInnerR) * uRingShadowSoftFrac, 1e-4);
    float pastInner = smoothstep(uRingInnerR - soft, uRingInnerR + soft, radial);
    float pastOuter = smoothstep(uRingOuterR - soft, uRingOuterR + soft, radial);

    return 1.0 - pastInner * (1.0 - pastOuter);
}

float cloudShadowLit(vec3 p)
{
    if (uCloudShadowStrength <= 0.0) return 1.0;

    vec3 shellHit = p + uSunDir * uCloudInner;
    vec3 nrm = shellHit * inversesqrt(dot(shellHit, shellHit));

    vec2 weather = sampleWeather(nrm);
    float coverage = clamp01(weather.x + uCoverageBias);

    float grazing = 1.0 / max(abs(dot(normalize(p), uSunDir)), 0.25);

    return 1.0 - clamp01(coverage * grazing * 0.65) * uCloudShadowStrength;
}

void main()
{
    vec4 tex = texture(uFaceTex, vUv);
    if (tex.a < 0.01) discard;

    vec3 n = normalize(vObjNormal);

    float lit = smoothstep(-uTerminatorSoftness, uTerminatorSoftness, dot(n, uSunDir));

    lit *= ringShadowLit(vObjPos);
    lit *= cloudShadowLit(vObjPos);

    float shade = uNightFloor + (1.0 - uNightFloor) * lit;

    fragColor = vec4(tex.rgb * vColor.rgb * shade, tex.a * vColor.a * uAlpha);
}
