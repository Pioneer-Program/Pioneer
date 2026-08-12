package cute.ame.pioneer.Core.Render.Helper;

import com.mojang.blaze3d.platform.GlStateManager;
import cute.ame.pioneer.Pioneer;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;

public final class SamplerBinder
{
    public static void bind(ShaderProgram shader, String uniformName, ResourceLocation texture, int unit)
    {
        int program = shader.getProgram();
        if (program == 0) return;

        int location = GL20.glGetUniformLocation(program, uniformName);
        if (location < 0)
        {
            Pioneer.LOGGER.warn("[Pioneer] sampler uniform '{}' not found in program {}", uniformName, program);
            return;
        }

        var tex = Minecraft.getInstance().getTextureManager().getTexture(texture, null);
        if (tex == null) return;

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(tex.getId());
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        GL41.glProgramUniform1i(program, location, unit);
    }

    public static void bindNearest(ShaderProgram shader, String uniformName, ResourceLocation texture, int unit)
    {
        int program = shader.getProgram();
        if (program == 0) return;

        int location = GL20.glGetUniformLocation(program, uniformName);
        if (location < 0)
        {
            Pioneer.LOGGER.warn("[Pioneer] sampler uniform '{}' not found in program {}", uniformName, program);
            return;
        }

        var tex = Minecraft.getInstance().getTextureManager().getTexture(texture, null);
        if (tex == null) return;

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(tex.getId());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        GL41.glProgramUniform1i(program, location, unit);
    }

    public static void setInt(ShaderProgram shader, String uniformName, int value)
    {
        int program = shader.getProgram();
        if (program == 0) return;

        int location = GL20.glGetUniformLocation(program, uniformName);
        if (location >= 0) GL41.glProgramUniform1i(program, location, value);
    }
}
