#version 150

#define PI 3.141592653589792
#define minBend 0.15
#define INV_SQRT3 0.5773502691896258

uniform float Fov;
uniform float _Size;
uniform float _Speed;
uniform float PhysicalRadius;
uniform float Scale;
uniform float Intensity;
uniform float _Steps;

uniform mat4 InvProjMatrix;
uniform mat4 InvViewModleMatrix;
uniform mat4 RotationMatrix;
uniform mat4 FullMatN;
uniform mat4 FullMatP;

uniform vec3 CameraPosition;
uniform vec3 BlackholePosition;
uniform vec3 Color;

uniform float GameTime;
uniform float RenderDistance;

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

float g_invSize;
float g_invSteps;
float g_effSpeed;
vec3  g_BHColor;
bool  g_customColor;

vec3 projectAndDivide(mat4 projectionMatrix, vec3 position)
{
    vec4 homogeneousPos = projectionMatrix * vec4(position, 1.0);
    return homogeneousPos.xyz / homogeneousPos.w;
}

vec3 screenToView(vec3 Position)
{
    return projectAndDivide(InvProjMatrix, Position * 2.0 - 1.0);
}

float cubeDist(vec3 p){ return max(abs(p.x), max(abs(p.y), abs(p.z))); }
float cubeDist(vec2 p){ return max(abs(p.x), abs(p.y)); }
float hash(float x){ return fract(sin(x) * 152754.742); }
float hash(vec2 x){ return hash(x.x + hash(x.y)); }

float value(vec2 p, float f)
{
    vec2 pf = p * f;
    vec2 ip = floor(pf);
    float bl = hash(ip);
    float br = hash(ip + vec2(1.0, 0.0));
    float tl = hash(ip + vec2(0.0, 1.0));
    float tr = hash(ip + vec2(1.0, 1.0));

    vec2 fr = fract(pf);
    fr = (3.0 - 2.0 * fr) * fr * fr;
    float b = mix(bl, br, fr.x);
    float t = mix(tl, tr, fr.x);
    return mix(b, t, fr.y);
}

vec4 background(vec3 ray)
{
    vec3 calcRay = (mat3(FullMatN) * ray) * mat3(RotationMatrix);

    vec3 absRay = abs(calcRay);
    vec2 uv;
    if (absRay.y >= absRay.x && absRay.y >= absRay.z)
    uv = calcRay.xz / calcRay.y;
    else if (absRay.x >= absRay.z)
    uv = calcRay.zy / calcRay.x;
    else
    uv = calcRay.xy / calcRay.z;

    float fovslope = tan(Fov * (PI / 360.0));
    uv = vec2(uv.x / (fovslope * (OutSize.x / OutSize.y)), uv.y / fovslope);

    float uvcubedist = cubeDist(uv);
    if (uvcubedist > 1.0)
    uv /= (uvcubedist * uvcubedist * uvcubedist);

    uv = fract(uv * 0.5 + 0.5);
    return texture(DiffuseSampler, uv);
}

vec4 raymarchDisk(vec3 ray, vec3 zeroPos)
{
    float invSize  = g_invSize;
    float invSteps = g_invSteps;
    float effSpeed = g_effSpeed;

    vec3 position = zeroPos;
    float lengthPos = cubeDist(position.xz);
    float invAbsRayY = 1.0 / abs(ray.y);
    float dist = min(1.0, lengthPos * invSize * 0.5) * _Size * 0.4 * invSteps * invAbsRayY;

    position += dist * _Steps * ray * 0.5;
    vec3 stepVec = dist * ray;

    vec2 deltaPos = vec2(-zeroPos.z * 0.01, zeroPos.x * 0.01);
    deltaPos = normalize(deltaPos);

    float parallel = dot(ray.xz, deltaPos) * (0.5 / sqrt(lengthPos));
    float redShift = parallel + 0.3;
    redShift = clamp(redShift * redShift, 0.0, 1.0);

    float disMix = clamp((lengthPos - _Size * 2.0) * invSize * 0.24, 0.0, 1.0);

    vec3 insideCol;
    if (!g_customColor)
    {
        insideCol  = mix(vec3(1.0, 0.8, 0.0), vec3(0.5, 0.13, 0.02) * 0.2, disMix);
        insideCol *= mix(vec3(0.4, 0.2, 0.1), vec3(1.6, 2.4, 4.0), redShift);
    }
    else
    {
        insideCol  = mix(g_BHColor, g_BHColor * 0.2, disMix);
        insideCol *= mix(g_BHColor * 0.8, g_BHColor * 2.0, redShift);
    }
    insideCol *= 10.85;

    float diskPulse = 1.5 + 0.5 * sin(GameTime * 2000.0) + 1.25 * sin(GameTime * 2.3 + 1.3);
    insideCol *= diskPulse;

    redShift += 0.26;
    redShift *= redShift;

    float rot = GameTime * effSpeed * 570.0;
    float sinRot = sin(rot);
    float cosRot = cos(rot);

    float gtCos = cos(GameTime * 2000.0) * 0.15 * 0.67;
    float uBase = GameTime * _Size * 0.3;
    float noiseTimeScale = GameTime * effSpeed * 3.5;
    float intensityCap = Intensity / 255.0;
    float alphaScale = (invSize * 10.0 + 0.01) * dist;
    const float f = 55.0;

    vec4 o = vec4(0.0);

    for (float i = 0.0; i < _Steps; i++)
    {
        if (o.a > 0.995) break;

        position -= stepVec;

        float intensity = clamp(1.0 - abs((i - 0.8) * invSteps * 2.0), 0.0, intensityCap);
        float lp = cubeDist(position.xz);

        float distMult = clamp((lp - _Size * 0.75) * invSize * 1.5, 0.0, 1.0)
        * clamp((_Size * 10.0 - lp) * invSize * 0.20, 0.0, 1.0);
        distMult *= distMult;

        float u = lp + uBase + intensity * _Size * 0.2;

        float rx = -position.z * sinRot + position.x * cosRot;
        float ry =  position.x * sinRot + position.z * cosRot;

        float angle = 0.0225 * atan(abs(rx / ry));

        float uScaled = u * invSize * 0.05;
        float noise = value(vec2(angle, uScaled) * noiseTimeScale, f);
        noise = noise + gtCos + 0.47 * value(vec2(angle, uScaled), f * 2.0);

        float extraWidth = noise * (1.0 - clamp(i * invSteps * 2.0 - 1.0, 0.0, 1.0));
        float alpha = clamp(noise * (intensity + extraWidth) * alphaScale * distMult, 0.0, 1.0);

        vec3 col = 2.0 * mix(vec3(0.3, 0.2, 0.15) * insideCol, insideCol, min(1.0, intensity * 2.0));
        float ia = 1.0 - alpha;
        o = clamp(vec4(col * alpha + o.rgb * ia, o.a * ia + alpha), vec4(0.0), vec4(1.0));

        float lpS = lp * invSize;
        o.rgb += redShift * (intensity + 0.5) * invSteps * 100.0 * distMult / (lpS * lpS);
    }

    o.rgb = clamp(o.rgb - 0.05, 0.3, 1.0);
    return o;
}

void main()
{
    g_invSize = 1.0 / _Size;
    g_invSteps = 1.0 / _Steps;
    g_effSpeed = (abs(_Speed) > 0.01) ? _Speed : 0.35;
    g_BHColor = Color;
    g_customColor = (Color.r >= 0.0);

    float fovslope = tan(Fov * (PI / 360.0));
    float aspectratio = OutSize.x / OutSize.y;

    vec2 ndcTex = texCoord * 2.0 - 1.0;
    vec3 dircordN = normalize(vec3(ndcTex.x * aspectratio * fovslope, ndcTex.y * fovslope, 1.0));

    vec3 bhOffset = BlackholePosition - CameraPosition;
    vec3 blhcord = normalize(bhOffset) * mat3(RotationMatrix);
    float bhangle = (1.0 - dot(dircordN, blhcord)) * 1.5;

    float bhDistance = length(bhOffset);
    float apparentRadius = PhysicalRadius / max(bhDistance, 0.01);
    float nocalcang = clamp(apparentRadius * 0.5, 0.05, 3.0);
    bool doWarp = bhDistance < 1000.0;

    if (bhangle < nocalcang)
    {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    float viewDepth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 viewPos = screenToView(vec3(texCoord, viewDepth));
    vec3 viewPosScreen = screenToView(vec3(texCoord, 1.0));
    vec3 PlayerViewPos = (InvViewModleMatrix * vec4(viewPosScreen, 1.0)).xyz;
    float viewDistance = length(viewPos);

    vec3 ray = mat3(FullMatP) * normalize(PlayerViewPos);
    vec3 pos = mat3(FullMatP) * (bhOffset / Scale);
    vec3 initalRay = ray;
    float totalBend = 0.0;

    vec4 col = vec4(0.0);
    vec4 glow = vec4(0.0);
    vec4 outCol = vec4(100.0);

    float sizeSq1000 = _Size * 1000.0;
    float sizeDiskThresh = _Size * 0.007;
    float haloPulse = 1.0 + 0.35 * sin(GameTime * 0.9);

    for (int disks = 0; disks < 32; disks++)
    {
        if (col.a > 0.995) {
            outCol = vec4(col.rgb + glow.rgb * glow.a, 1.0);
            break;
        }

        for (int h = 0; h < 6; h++)
        {
            float centDist = cubeDist(pos);
            float invDist = 1.0 / centDist;
            float invDistSqr = invDist * invDist;

            float stepDist = 0.92 * abs(pos.y / ray.y);
            float farLimit = centDist * 0.5;
            float closeLimit = centDist * 0.1 + 0.05 * centDist * centDist * g_invSize;
            stepDist = min(stepDist, min(farLimit, closeLimit));

            float bendForce = stepDist * invDistSqr * _Size * 0.625;
            vec3 bender = (bendForce * invDist) * pos;
            vec3 rb = ray - bender;
            ray = rb / cubeDist(rb);
            totalBend += cubeDist(bender);

            pos += stepDist * (ray * INV_SQRT3);

            float glowFactor = 0.028 * haloPulse * stepDist * invDistSqr * invDistSqr
            * clamp(centDist * 3.0 - 1.2, 0.0, 1.0);

            if (!g_customColor)
            {
                glow += vec4(1.2, 1.1, 1.0, 1.0) * glowFactor;
            }
            else
            {
                float redShift = clamp(dot(ray.xz, normalize(vec2(-pos.z, pos.x)))
                                       / max(1.0, length(pos.xz)), 0.0, 1.0);
                redShift *= redShift;
                vec3 glowColor = mix(g_BHColor, g_BHColor * 2.5, redShift);
                glow += vec4(glowColor, 1.0) * (glowFactor * (0.128 / 0.028));
            }
        }

        float dist2 = length(pos);

        if (dist2 < _Size)
        {
            outCol = vec4(col.rgb * col.a + glow.rgb * (1.0 - col.a), 1.0);
            break;
        }
        else if (dist2 > sizeSq1000)
        {
            float ts = clamp((totalBend - minBend) * 7.0, 0.0, 1.0);
            ts = (3.0 - 2.0 * ts) * ts * ts;
            vec3 smoothedRay = mix(initalRay, ray, ts);
            float invA = 1.0 - col.a;

            if (totalBend > minBend && doWarp) {
                vec4 bg = background(normalize(smoothedRay));
                outCol = vec4(col.rgb * col.a + bg.rgb * invA + glow.rgb * invA, 1.0);
            } else {
                vec3 sc = texture(DiffuseSampler, texCoord).rgb;
                outCol = vec4(col.rgb * col.a + sc * invA + glow.rgb * invA, 1.0);
            }
            break;
        }
        else if (abs(pos.y) <= sizeDiskThresh)
        {
            vec4 diskCol = raymarchDisk(ray, pos);
            pos.y = 0.0;
            pos += abs(_Size * 0.001 / ray.y) * ray;
            float invA = 1.0 - col.a;
            col = vec4(diskCol.rgb * invA + col.rgb, col.a + diskCol.a * invA);
        }
    }

    if (outCol.r == 100.0)
    outCol = vec4(col.rgb + glow.rgb * (col.a + glow.a), col.a);

    col = outCol;

    float blackholeDistance = bhDistance;
    vec3 sceneColor = texture(DiffuseSampler, texCoord).rgb;
    vec3 finalColor;

    if (blackholeDistance < viewDistance) {
        float effAlpha = clamp(col.a + glow.a * 0.05, 0.0, 1.0);
        finalColor = (col.rgb + glow.rgb) * effAlpha + sceneColor * (1.0 - effAlpha);
    } else {
        finalColor = viewDistance < RenderDistance * 16.0 ? sceneColor : col.rgb + glow.rgb;
    }

    fragColor = vec4(finalColor, 1.0);
}