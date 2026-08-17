package cute.ame.pioneer.Core.Render.Baking.Planet;

import com.mojang.blaze3d.platform.NativeImage;
import cute.ame.pioneer.Pioneer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class RingTextureHelper
{
    private static ResourceLocation WHITE = null;

    public static ResourceLocation getWhite()
    {
        if (WHITE != null) return WHITE;

        NativeImage img = new NativeImage(1, 1, false);
        img.setPixelRGBA(0, 0, 0xFFFFFFFF);

        DynamicTexture tex = new DynamicTexture(img);
        WHITE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "generated/ring_white");
        Minecraft.getInstance().getTextureManager().register(WHITE, tex);
        return WHITE;
    }
}
