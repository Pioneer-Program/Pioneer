package cute.ame.pioneer.Core.Render.Baking.LUT;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Core.Render.Baking.LUT.Bakers.CloudVolumeBaker;
import cute.ame.pioneer.Core.Render.Baking.LUT.Bakers.CloudWeatherBaker;
import cute.ame.pioneer.Core.Render.Baking.LUT.Bakers.GalaxySkyboxBaker;
import net.minecraft.resources.ResourceLocation;

public final class BuiltinLUTs
{
    public static final int CLOUD_VOLUME_RES = 64;
    public static final int CLOUD_WEATHER_RES = 256;
    public static final int GALAXY_SKYBOX_RES = 2048;

    public static final ResourceLocation CLOUD_VOLUME = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "lut/cloud_volume");
    public static final ResourceLocation CLOUD_WEATHER = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "lut/cloud_weather");
    public static final ResourceLocation GALAXY_SKYBOX = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "lut/galaxy_skybox");

    public static void registerAll()
    {
        LUTRegistry.register(CLOUD_VOLUME,  new CloudVolumeBaker());
        LUTRegistry.register(CLOUD_WEATHER, new CloudWeatherBaker());
        LUTRegistry.register(GALAXY_SKYBOX, new GalaxySkyboxBaker());
    }
}
