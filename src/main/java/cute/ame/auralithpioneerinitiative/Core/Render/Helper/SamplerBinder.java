package cute.ame.auralithpioneerinitiative.Core.Render.Helper;

import com.mojang.blaze3d.platform.GlStateManager;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
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
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] sampler uniform '{}' not found in program {}", uniformName, program);
            return;
        }

        var tex = Minecraft.getInstance().getTextureManager().getTexture(texture, null);
        if (tex == null) return;

        GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
        GlStateManager._bindTexture(tex.getId());
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
