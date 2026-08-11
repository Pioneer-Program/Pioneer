package cute.ame.pioneer.Registrie;

import cute.ame.pioneer.Core.Compat.VeilSkyShaderHelper;
import net.minecraft.resources.ResourceLocation;

public class ModVfxShaders
{
    public static void registersAll()
    {
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("pioneer", "jet_flare"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("pioneer", "atmosphere"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("pioneer", "rings"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("pioneer", "accretion_disk"));
    }
}
