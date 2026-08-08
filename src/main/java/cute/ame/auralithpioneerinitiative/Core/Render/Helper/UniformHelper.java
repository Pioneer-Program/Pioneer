package cute.ame.auralithpioneerinitiative.Core.Render.Helper;

import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import org.joml.Matrix4f;

public final class UniformHelper
{
    public static void set(ShaderProgram shader, String name, float v)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setFloat(v);
    }

    public static void set(ShaderProgram shader, String name, float x, float y)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setVector(x, y);
    }

    public static void set(ShaderProgram shader, String name, float x, float y, float z)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setVector(x, y, z);
    }

    public static void set(ShaderProgram shader, String name, Matrix4f m)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setMatrix(m);
    }

    public static void setInt(ShaderProgram shader, String name, int value)
    {
        SamplerBinder.setInt(shader, name, value);
    }
}
