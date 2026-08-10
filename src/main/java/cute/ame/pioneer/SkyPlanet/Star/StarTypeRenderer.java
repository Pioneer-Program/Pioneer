package cute.ame.pioneer.SkyPlanet.Star;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.pioneer.SkyPlanet.Data.SunDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface StarTypeRenderer
{
    void renderExtras(PoseStack ps, SunDefinition sun, long tick, float partialTick, float apparentSize, MultiBufferSource.BufferSource bufferSource, double dx, double dy, double dz, Vec3 realCamPos);
}