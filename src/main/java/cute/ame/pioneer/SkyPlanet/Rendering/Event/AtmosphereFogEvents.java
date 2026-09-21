package cute.ame.pioneer.SkyPlanet.Rendering.Event;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Rendering.gl.AtmosphereRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class AtmosphereFogEvents
{
    private static final float[] COLOR = new float[3];

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event)
    {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
        if (event.getCamera().getEntity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
        if (!AtmosphereRenderer.horizonFogColor(COLOR)) return;

        event.setRed(COLOR[0]);
        event.setGreen(COLOR[1]);
        event.setBlue(COLOR[2]);
    }
}
