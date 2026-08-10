#version 150

in vec3 vObjPos;
in vec2 vUv;
in vec4 vColor;

uniform vec3  uSunDir;
uniform float uPlanetRadius;
uniform float uInnerR;
uniform float uOuterR;
uniform float uShadowSoftFrac;
uniform float uShadowFloor;

uniform float uBandCount;
uniform float uBandContrast;
uniform float uSeed;

out vec4 fragColor;

float clamp01(float v)
{

    return clamp(v, 0.0, 1.0); }

float hash11(float x)
{
    return fract(sin(x * 127.1) * 43758.5453);
}

float planetShadowLit(vec3 p)
{
    float dotPS = dot(p.xz, uSunDir.xz);
    float distSq = dot(p.xz, p.xz);

    float d = sqrt(max(0.0, distSq - dotPS * dotPS));

    float soft = max(uPlanetRadius * uShadowSoftFrac, 1e-4);
    float dMiss = smoothstep(uPlanetRadius - soft, uPlanetRadius + soft, d);
    float facingSun = smoothstep(-soft, soft, dotPS);

    float litness = 1.0 - (1.0 - dMiss) * (1.0 - facingSun);
    return uShadowFloor + (1.0 - uShadowFloor) * litness;
}

void main()
{
    float radial = length(vObjPos.xz);
    if (radial < uInnerR || radial > uOuterR) discard;

    float span = max(uOuterR - uInnerR, 1e-4);
    float edge = min(smoothstep(0.0, 0.04, (radial - uInnerR) / span), smoothstep(0.0, 0.04, (uOuterR - radial) / span));

    float band = 1.0;
    if (uBandCount > 0.0)
    {
        float bandIdx = floor((radial - uInnerR) / span * uBandCount);
        band = mix(1.0 - uBandContrast, 1.0, hash11(bandIdx + uSeed));
    }

    float shade = planetShadowLit(vObjPos);

    vec3 rgb = vColor.rgb * shade * band;
    float a = vColor.a * edge;

    if (a < 0.002) discard;
    fragColor = vec4(rgb, a);
}
