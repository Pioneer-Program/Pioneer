package cute.ame.pioneer.SkyPlanet.Rendering.PostProcess;

import com.mojang.logging.LogUtils;
import cute.ame.pioneer.SkyPlanet.Data.BlackHoleDefinition;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.stage.BlitPostStage;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.util.Collections;

public final class BlackHolePostProcessRenderer
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SHADER_ID = ResourceLocation.fromNamespaceAndPath("pioneer", "black_hole");

    private static final BlitPostStage STAGE = new BlitPostStage(SHADER_ID, Collections.emptyMap(), VeilFramebuffers.MAIN, VeilFramebuffers.POST, true);

    private static boolean warnedMissingShader = false;

    public static void renderIfActive(Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick)
    {
        BlackHolePostProcessManager.Active active = BlackHolePostProcessManager.consume();
        if (active == null) return;

        ShaderProgram shader = VeilRenderSystem.renderer().getShaderManager().getShader(SHADER_ID);
        if (shader == null)
        {
            if (!warnedMissingShader)
            {
                warnedMissingShader = true;
                LOGGER.warn("[Auralith] black_hole post-process shader not resolved yet (still compiling / failed to load) — skipping this frame");
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BlackHoleDefinition def = active.definition();
        Vec3 camPos = camera.getPosition();
        Vec3 holePos = active.worldPosition();

        Matrix4f rotOnly = new Matrix4f(frustumMatrix);
        rotOnly.m03(0.0f).m13(0.0f).m23(0.0f).m30(0.0f).m31(0.0f).m32(0.0f);
        Matrix4f invRot = new Matrix4f(rotOnly).invert();
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Matrix4f identity = new Matrix4f();

        setMatrix(shader, "ProjMat", projectionMatrix);
        setMatrix(shader, "InvProjMatrix", invProj);
        setMatrix(shader, "InvViewModleMatrix", invRot);
        setMatrix(shader, "RotationMatrix", rotOnly);
        float tiltRad = (float) Math.toRadians(def.diskTilt());
        Matrix4f tilt = new Matrix4f().rotationX(tiltRad);
        setMatrix(shader, "FullMatP", tilt);
        setMatrix(shader, "FullMatN", tilt);
        setVec3(shader, "CameraPosition", (float) camPos.x, (float) camPos.y, (float) camPos.z);
        setVec3(shader, "BlackholePosition", (float) holePos.x, (float) holePos.y, (float) holePos.z);
        setVec3(shader, "Color", def.r(), def.g(), def.b());
        float gameTime = (mc.level != null ? mc.level.getGameTime() : 0L) + partialTick;
        setFloat(shader, "GameTime", gameTime);

        float fixedScale = Math.max(def.scale(), 1e-3f);
        float physicalRadius = def.size() * fixedScale;

        setFloat(shader, "PhysicalRadius", physicalRadius);
        setFloat(shader, "Scale", fixedScale);
        setFloat(shader, "_Size", def.size());
        setFloat(shader, "_Steps", def.steps());
        setFloat(shader, "_Speed", def.speed());
        setFloat(shader, "Intensity", def.intensity());
        setFloat(shader, "RenderDistance", mc.options.renderDistance().get());
        setFloat(shader, "Fov", (float) 101.0f);

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        ShaderUniformAccess outSize = shader.getUniform("OutSize");
        if (outSize != null) outSize.setVector((float) width, (float) height);

        VeilRenderSystem.renderer().getPostProcessingManager().runPipeline(STAGE, true);
    }

    private static void setMatrix(ShaderProgram shader, String name, Matrix4f value)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setMatrix(value);
    }

    private static void setVec3(ShaderProgram shader, String name, float x, float y, float z)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setVector(x, y, z);
    }

    private static void setFloat(ShaderProgram shader, String name, float value)
    {
        ShaderUniformAccess u = shader.getUniform(name);
        if (u != null) u.setFloat(value);
    }
}