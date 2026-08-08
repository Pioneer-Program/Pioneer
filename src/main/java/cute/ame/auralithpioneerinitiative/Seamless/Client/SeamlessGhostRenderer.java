package cute.ame.auralithpioneerinitiative.Seamless.Client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import cute.ame.auralithpioneerinitiative.Mixin.MinecraftLevelRendererAccessor;
import cute.ame.auralithpioneerinitiative.Seamless.SeamlessLevelRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SeamlessGhostRenderer
{
    private static double savedPlayerX, savedPlayerY, savedPlayerZ;
    private static float savedPlayerYRot, savedPlayerXRot;

    public static volatile boolean IN_GHOST_DEBUG_FRAME = false;

    private static RenderTarget bridgeTarget;
    private static int bridgeTargetWidth = -1, bridgeTargetHeight = -1;
    private static volatile boolean bridgeActive = false;
    private static ResourceKey<Level> bridgeDimension = null;

    public static void setBridgeTarget(ResourceKey<Level> dimension)
    {
        bridgeDimension = dimension;
        bridgeActive = dimension != null;
    }

    public static boolean isBridgeActive()
    {
        return bridgeActive;
    }

    private static RenderTarget getOrCreateBridgeTarget(int width, int height)
    {
        if (bridgeTarget == null || width != bridgeTargetWidth || height != bridgeTargetHeight)
        {
            if (bridgeTarget != null) bridgeTarget.destroyBuffers();
            bridgeTarget = new com.mojang.blaze3d.pipeline.TextureTarget(width, height, true, Minecraft.ON_OSX);
            bridgeTargetWidth = width;
            bridgeTargetHeight = height;
        }
        return bridgeTarget;
    }

    public static void renderBridgeFrame()
    {
        if (!bridgeActive || bridgeDimension == null) return;

        ClientLevel ghostLevel = SeamlessLevelRegistry.get(bridgeDimension).orElse(null);
        if (ghostLevel == null)
        {
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] Bridge target {} no longer retained in SeamlessLevelRegistry, ending bridge", bridgeDimension.location());
            setBridgeTarget(null);
            return;
        }

        BlockPos anchor = SeamlessGhostAnchors.get(bridgeDimension);
        if (anchor == null)
        {
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] No known anchor for {}, ending bridge", bridgeDimension.location());
            setBridgeTarget(null);
            return;
        }

        SeamlessGhostLevelBuilder.resyncGameTime(ghostLevel);

        Minecraft mc = Minecraft.getInstance();
        RenderTarget realMainTarget = mc.getMainRenderTarget();
        int w = realMainTarget.width, h = realMainTarget.height;
        RenderTarget target = getOrCreateBridgeTarget(w, h);
        try
        {
            SeamlessGhostSectionRenderer.render(target, ghostLevel, bridgeDimension, anchor, w, h);
        }
        catch (Exception e)
        {
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] renderBridgeFrame failed, ending bridge early: {}", e.toString());
            setBridgeTarget(null);
        }
        finally
        {
            realMainTarget.bindWrite(true);
        }
    }

    private static final ResourceLocation BRIDGE_TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "seamless_bridge_target");
    private static boolean bridgeTextureRegistered = false;

    public static void blitBridgeOverlay(GuiGraphics guiGraphics)
    {
        if (!bridgeActive || bridgeTarget == null) return;

        Minecraft mc = Minecraft.getInstance();

        if (!bridgeTextureRegistered)
        {
            mc.getTextureManager().register(BRIDGE_TEXTURE_LOCATION, new SeamlessExternalGlTexture(bridgeTarget.getColorTextureId()));
            bridgeTextureRegistered = true;
        }

        int w = guiGraphics.guiWidth(), h = guiGraphics.guiHeight();
        guiGraphics.blit(BRIDGE_TEXTURE_LOCATION, 0, 0, 0f, 0f, w, h, w, h);
    }

    public static LevelRenderer sharedLevelRenderer()
    {
        return Minecraft.getInstance().levelRenderer;
    }

    public static synchronized void renderSingleDebugFrame(ClientLevel ghostLevel)
    {
        Minecraft mc = Minecraft.getInstance();
        MinecraftLevelRendererAccessor accessor = (MinecraftLevelRendererAccessor) mc;
        ClientLevel realLevel = accessor.auralith$getLevel();

        boolean entered = false;
        IN_GHOST_DEBUG_FRAME = true;
        try
        {
            entered = enterGhostLevel(ghostLevel, realLevel, mc, accessor, true);
            if (!entered) return;

            final long MAX_WAIT_MS = 3000;
            final long POLL_INTERVAL_MS = 50;
            long deadline = System.currentTimeMillis() + MAX_WAIT_MS;

            while (System.currentTimeMillis() < deadline)
            {

                mc.gameRenderer.render(DeltaTracker.ONE, true);

                try { Thread.sleep(POLL_INTERVAL_MS); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }

            }

            try
            {
                String fileName = "ghost_debug_" + ghostLevel.dimension().location().getPath() + "_" + System.currentTimeMillis() + ".png";
                Screenshot.grab(mc.gameDirectory, fileName, mc.getMainRenderTarget(), component -> Auralithpioneerinitiative.LOGGER.info("[Auralith] Ghost debug frame captured: {}", component.getString()));
            }
            catch (Exception screenshotEx)
            {
                Auralithpioneerinitiative.LOGGER.warn("[Auralith] Ghost debug frame rendered but screenshot capture failed: {}", screenshotEx.toString());
            }
        }
        catch (Exception e)
        {
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] renderSingleDebugFrame failed while rendering the swapped-in frame: {}", e.toString());
        }
        finally
        {

            if (entered)
            {
                exitGhostLevel(realLevel, mc, accessor);
            }
            IN_GHOST_DEBUG_FRAME = false;
        }
    }

    private static boolean enterGhostLevel(ClientLevel ghostLevel, ClientLevel realLevel, Minecraft mc, MinecraftLevelRendererAccessor accessor, boolean shouldRebuild)
    {
        try
        {
            accessor.auralith$setLevel(ghostLevel);
            mc.levelRenderer.setLevel(ghostLevel);

            if (shouldRebuild)
            {
                mc.levelRenderer.allChanged();
            }
        }
        catch (Exception e)
        {

            Auralithpioneerinitiative.LOGGER.warn("[Auralith] enterGhostLevel failed, attempting to restore real level: {}", e.toString());
            accessor.auralith$setLevel(realLevel);
            return false;
        }

        LocalPlayer player = mc.player;
        BlockPos anchor = SeamlessGhostAnchors.get(ghostLevel.dimension());
        if (player != null && anchor != null)
        {
            savedPlayerX = player.getX();
            savedPlayerY = player.getY();
            savedPlayerZ = player.getZ();
            savedPlayerYRot = player.getYRot();
            savedPlayerXRot = player.getXRot();

            int surfaceY = ghostLevel.getHeight(Heightmap.Types.WORLD_SURFACE, anchor.getX(), anchor.getZ());

            player.setPos(anchor.getX() + 0.5, surfaceY + 2.0, anchor.getZ() + 0.5);

            player.setXRot(30.0f);
            Auralithpioneerinitiative.LOGGER.debug("[Auralith] Entered ghost level mode for one frame, camera moved to anchor XZ {},{} at ghost-level surface height y={} (anchor.y={} was ignored — see comment)", anchor.getX(), anchor.getZ(), surfaceY, anchor.getY());
        }
        else if (player != null)
        {
            Auralithpioneerinitiative.LOGGER.warn("[Auralith] Entered ghost level mode but no known anchor for {} — camera staying at the real player's position, ghost terrain likely won't be visible", ghostLevel.dimension().location());
        }

        return true;
    }

    private static void exitGhostLevel(ClientLevel realLevel, Minecraft mc, MinecraftLevelRendererAccessor accessor)
    {
        accessor.auralith$setLevel(realLevel);
        mc.levelRenderer.setLevel(realLevel);
        mc.levelRenderer.allChanged();

        LocalPlayer player = mc.player;
        if (player != null)
        {
            player.setPos(savedPlayerX, savedPlayerY, savedPlayerZ);
            player.setYRot(savedPlayerYRot);
            player.setXRot(savedPlayerXRot);
        }

        Auralithpioneerinitiative.LOGGER.debug("[Auralith] Exited ghost level mode, real level + camera restored");
    }
}