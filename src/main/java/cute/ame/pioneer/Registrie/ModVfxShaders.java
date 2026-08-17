package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

public class ModVfxShaders
{
    public static void registersAll()
    {
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "jet_flare"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "atmosphere"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "rings"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "body"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "star"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "accretion_disk"));
    }
}
