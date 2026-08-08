package cute.ame.auralithpioneerinitiative.Registrie;

import cute.ame.auralithpioneerinitiative.Core.Compat.VeilSkyShaderHelper;
import net.minecraft.resources.ResourceLocation;

public class ModVfxShaders
{
    public static void registersAll()
    {
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "jet_flare"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "atmosphere"));
        VeilSkyShaderHelper.registerVfxShader(ResourceLocation.fromNamespaceAndPath("auralithpioneerinitiative", "accretion_disk"));
    }
}
