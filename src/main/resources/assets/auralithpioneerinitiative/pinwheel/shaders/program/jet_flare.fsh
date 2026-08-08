#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform float uTime;
uniform float uDir;
uniform float uWobbleSpeed;
uniform float uHelixTurns;
uniform float uFiberDistortion;
uniform float uTrailPersistence;

out vec4 fragColor;

const float PI = 3.14159265359;
const float STRIPE_WIDTH = 0.32;
const int   TRAIL_ECHOES = 2;
const float TRAIL_SPACING = 0.4;

float sinN(float x) { return sin(x) * 0.5 + 0.5; }

void main() {
    float lengthFrac = texCoord0.x;
    float angle = texCoord0.y * 2.0 * PI;

    float spinAngle = uTime * 2.0 * PI * uWobbleSpeed;
    float stripeAngle = uDir * lengthFrac * uHelixTurns * 2.0 * PI + spinAngle;

    float boost = 0.0;
    for (int k = 0; k <= TRAIL_ECHOES; k++) {
        float echoAngle = stripeAngle - float(k) * TRAIL_SPACING;
        float delta = atan(sin(angle - echoAngle), cos(angle - echoAngle));
        float band = 1.0 - smoothstep(0.0, STRIPE_WIDTH, abs(delta));
        boost += band * pow(uTrailPersistence, float(k));
    }
    boost = min(boost, 1.0);

    float fiberClock = uTime * uWobbleSpeed;
    float helixBase = uHelixTurns * 2.0 * PI;
    float spin = fiberClock * 2.0 * PI;

    float fiber = 0.0;
    fiber += 0.45 * sinN(uDir * lengthFrac * helixBase * 1.0 - angle + spin * uDir);
    fiber += 0.30 * sinN(uDir * lengthFrac * helixBase * 2.0 - angle * 2.0 + spin * uDir * 2.0 + 1.2);
    fiber += 0.25 * sinN(uDir * lengthFrac * helixBase * 3.0 - angle * 3.0 + spin * uDir * 3.0 + 2.4);
    fiber = min(fiber, 1.0);

    float fiberAlpha = (fiber - 0.5) * 2.0 * uFiberDistortion;
    float alphaMul = max(0.25, 1.0 + fiberAlpha * 0.6);

    float bgFade = 1.0 - (abs(lengthFrac - 0.5) * 2.0);
    bgFade = smoothstep(0.0, 1.0, bgFade);

    vec3 baseBgColor = mix(vertexColor.rgb, vec3(1.0), fiber * 0.10);
    vec3 trailColor = vec3(1.0);

    vec3 finalColor = baseBgColor * (vertexColor.a * alphaMul * bgFade);
    finalColor += trailColor * (boost * 0.35);

    float finalAlpha = (vertexColor.a * alphaMul * bgFade) + (boost * 0.25);

    fragColor = vec4(finalColor, min(finalAlpha, 1.0));
}