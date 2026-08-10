package cute.ame.pioneer.Seamless.Client;

import net.minecraft.client.renderer.texture.AbstractTexture;

public final class SeamlessExternalGlTexture extends AbstractTexture
{
    public SeamlessExternalGlTexture(int existingGlId)
    {
        this.id = existingGlId;
    }

    @Override
    public void load(net.minecraft.server.packs.resources.ResourceManager resourceManager)
    {
    }

    @Override
    public void releaseId()
    {
    }
}
