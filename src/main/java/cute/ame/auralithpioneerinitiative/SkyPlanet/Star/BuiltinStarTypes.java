package cute.ame.auralithpioneerinitiative.SkyPlanet.Star;

import com.mojang.blaze3d.vertex.PoseStack;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Data.SunDefinition;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.PostProcess.BlackHolePostProcessManager;
import cute.ame.auralithpioneerinitiative.SkyPlanet.Rendering.gl.JetConeRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class BuiltinStarTypes
{
    public static final ResourceLocation MAIN_SEQUENCE = id("main_sequence");
    public static final ResourceLocation RED_GIANT = id("red_giant");
    public static final ResourceLocation WHITE_DWARF = id("white_dwarf");
    public static final ResourceLocation NEUTRON_STAR = id("neutron_star");
    public static final ResourceLocation BLACK_HOLE = id("black_hole");

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", path);
    }

    public static void registerAll()
    {
        StarTypeRegistry.register(MAIN_SEQUENCE, (ps, sun, tick, partialTick, apparentSize, bufferSource, dx, dy, dz, realCamPos) -> {});
        StarTypeRegistry.register(RED_GIANT, (ps, sun, tick, partialTick, apparentSize, bufferSource, dx, dy, dz, realCamPos) -> {});
        StarTypeRegistry.register(WHITE_DWARF, BuiltinStarTypes::renderJetsAndDiskIfPresent);
        StarTypeRegistry.register(NEUTRON_STAR, BuiltinStarTypes::renderJetsAndDiskIfPresent);
        StarTypeRegistry.register(BLACK_HOLE, BuiltinStarTypes::renderBlackHole);
    }

    private static void renderJetsAndDiskIfPresent(PoseStack ps, SunDefinition sun, long tick, float partialTick, float apparentSize, MultiBufferSource.BufferSource bufferSource, double dx, double dy, double dz, Vec3 realCamPos)
    {
        if (sun.jetCone() != null) JetConeRenderer.render(ps, sun.jetCone(), tick, partialTick, apparentSize, bufferSource);
    }

    private static void renderBlackHole(PoseStack ps, SunDefinition sun, long tick, float partialTick, float apparentSize, MultiBufferSource.BufferSource bufferSource, double dx, double dy, double dz, Vec3 realCamPos)
    {
        if (sun.blackHole() == null)
        {
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] Sun uses black_hole type but has no black_hole config -- nothing to lens");
            return;
        }

        Vec3 worldPos = realCamPos.add(dx, dy, dz);
        BlackHolePostProcessManager.report(worldPos, sun.blackHole());
    }
}