#version 150

uniform sampler2D uSkyboxLUT;

uniform float uTime;
uniform mat4 uInvViewProj;
uniform float uScreenWidth;
uniform float uScreenHeight;
uniform float uStarVisibility;

out vec4 fragColor;

vec2 octaEncode(vec3 n)
{
    n *= 1.0 / (abs(n.x) + abs(n.y) + abs(n.z) + 1e-6);
    vec2 uv = n.xz;
    if (n.y < 0.0)uv = (1.0 - abs(uv.yx)) * vec2(uv.x >= 0.0 ? 1.0 : -1.0, uv.y >= 0.0 ? 1.0 : -1.0);
    return uv * 0.5 + 0.5;
}

void main()
{
    vec2 ndc = (gl_FragCoord.xy / vec2(uScreenWidth, uScreenHeight)) * 2.0 - 1.0;
    vec4 nearPoint = uInvViewProj * vec4(ndc, -1.0, 1.0);
    vec4 farPoint = uInvViewProj * vec4(ndc,  1.0, 1.0);
    nearPoint /= nearPoint.w;
    farPoint /= farPoint.w;
    vec3 dir = normalize(farPoint.xyz - nearPoint.xyz);

    vec4 sky = texture(uSkyboxLUT, octaEncode(dir));
    float phase = dot(dir, vec3(37.1, 61.7, 23.3));
    float twinkle = 0.85 + 0.15 * sin(uTime * 1.1 + phase);

    vec3 col = sky.rgb * (1.0 + sky.a * (twinkle - 1.0));
    fragColor = vec4(col * uStarVisibility, 1.0);
}
