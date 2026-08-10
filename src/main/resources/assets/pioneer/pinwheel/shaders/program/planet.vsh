#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vObjPos;
out vec3 vObjNormal;
out vec2 vUv;
out vec4 vColor;

void main()
{
    vObjPos = Position;
    vObjNormal = Normal;
    vUv = UV0;
    vColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
