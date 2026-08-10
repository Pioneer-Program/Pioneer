package cute.ame.auralithpioneerinitiative.Core.Render.Debug;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.minecraft.ChatFormatting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Auralithpioneerinitiative.MODID, value = Dist.CLIENT)
public final class GPUProfilerOverlay
{
    private static final double NOISE_FLOOR_MILLIS = 0.01;

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event)
    {
        if (!GPUProfiler.isEnabled()) return;

        Map<String, Double> sections = GPUProfiler.snapshot();
        if (sections.isEmpty()) return;

        List<String> right = event.getLeft();
        right.add("");
        right.add("Pioneer components GPU usage");

        int hidden = 0;
        for (Map.Entry<String, Double> e : sections.entrySet())
        {
            double ms = e.getValue();
            if (ms < NOISE_FLOOR_MILLIS)
            {
                hidden++;
                continue;
            }

            right.add(String.format("%s %s%.3f ms", e.getKey(), colourFor(ms), ms));
        }

        if (hidden > 0) right.add(ChatFormatting.GRAY + String.format("%d section(s) < %.2f ms", hidden, NOISE_FLOOR_MILLIS));

        double total = GPUProfiler.totalMillis();
        right.add(String.format("%stotal %s%.3f ms", ChatFormatting.GRAY, colourFor(total), total));
    }

    private static ChatFormatting colourFor(double millis)
    {
        if (millis >= 8.0) return ChatFormatting.RED;
        if (millis >= 2.0) return ChatFormatting.YELLOW;
        return ChatFormatting.WHITE;
    }
}