package cute.ame.pioneer.Mixin.Rendering;

import cute.ame.pioneer.Seamless.SeamlessTransitionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class SeamlessLoadingScreenMixin
{
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void pioneer$suppressDuringSeamlessTransition(Screen screen, CallbackInfo ci)
    {
        if (!SeamlessTransitionState.isTransitioning()) return;
        if (screen instanceof ReceivingLevelScreen) ci.cancel();
    }
}