package cute.ame.auralithpioneerinitiative.Core.Compat;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class VeilSkyShaderHelper
{
    public static void registerVfxShader(ResourceLocation shaderPath)
    {
        IrisVeilCompatHook.tryExcludeShaderReplacement(shaderPath);
    }

    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    public static void draw(ResourceLocation shaderPath, Consumer<ShaderProgram> uniformSetup, Consumer<RenderType> geometryAndDraw)
    {
        RenderType renderType = VeilRenderType.get(shaderPath);
        if (renderType == null)
        {
            if (WARNED.add(shaderPath)) Auralithpioneerinitiative.LOGGER.warn("VeilRenderType.get({}) returned null, render type JSON missing/failed to parse, drawing nothing T*T", shaderPath);
            return;
        }

        ShaderProgram shader = VeilRenderSystem.setShader(shaderPath);
        if (shader == null)
        {
            if (WARNED.add(ResourceLocation.fromNamespaceAndPath(shaderPath.getNamespace(), shaderPath.getPath() + "_shadernull"))) Auralithpioneerinitiative.LOGGER.warn("VeilRenderSystem.setShader({}) returned null, shader program failed to compile/resolve.. skipping draw this frame :c", shaderPath);
            return;
        }
        if (uniformSetup != null) uniformSetup.accept(shader);

        IrisGbufferSync.syncGbufferTargetIfNeeded();
        geometryAndDraw.accept(renderType);
    }
}