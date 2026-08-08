#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform mat4 uPlanetModel;

out vec3 vObjPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * uPlanetModel * vec4(Position, 1.0);
    vObjPos = Position;
}
