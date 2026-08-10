package cute.ame.pioneer.Seamless;

import cute.ame.pioneer.Pioneer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class SeamlessFadeRenderer
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (!SeamlessFadeState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        var expected = SeamlessFadeState.expectedTargetDim();
        boolean matches = mc.level != null && expected != null && mc.level.dimension().equals(expected);
        SeamlessFadeState.tick(matches);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        float alpha = SeamlessFadeState.alpha();
        if (alpha <= 0f) return;

        int a = Math.round(Math.min(1f, alpha) * 255f);
        int color = (a << 24);
        var guiGraphics = event.getGuiGraphics();
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), color);
    }
}
