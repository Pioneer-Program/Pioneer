package cute.ame.auralithpioneerinitiative.Mixin.Rendering;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
    @Inject(method = {"getDepthFar"}, at = {@At("HEAD")}, cancellable = true)
    public void onGetDepthFar(CallbackInfoReturnable<Float> ci)
    {
        ci.setReturnValue(Float.POSITIVE_INFINITY);
    }
}