package cute.ame.pioneer.Core.Render.Baking.LUT;

import com.mojang.blaze3d.platform.NativeImage;

@FunctionalInterface
public interface LUTBaker
{
    NativeImage bake(LUTParams params);

    default boolean linearFilter()
    {
        return true;
    }
}
