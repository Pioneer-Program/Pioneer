package cute.ame.pioneer.Core.Render.Baking.Planet;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Core.Render.Baking.Planet.Bakers.*;
import net.minecraft.resources.ResourceLocation;

public final class BuiltinPlanetTextures
{
    public static void registerAll()
    {
        register("rocky", new RockyTextureBaker());
        register("gas_giant", new GasGiantTextureBaker());
        register("ocean", new OceanTextureBaker());
        register("ice", new IceTextureBaker());
        register("lava", new LavaTextureBaker());
        register("telluric", new TelluricTextureBaker());
    }

    private static void register(String path, PlanetTextureBaker generator)
    {
        PlanetTextureRegistry.register(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, path), generator);
    }
}
