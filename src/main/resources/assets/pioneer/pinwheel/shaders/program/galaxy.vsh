#version 150

in vec3 Position;

uniform mat4 ProjMat;

void main()
{
    vec4 clip = ProjMat * vec4(Position, 1.0);
    clip.z = clip.w;
    gl_Position = clip;
}
