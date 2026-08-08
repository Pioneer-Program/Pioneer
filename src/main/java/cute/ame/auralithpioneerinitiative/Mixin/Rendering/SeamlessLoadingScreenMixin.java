package cute.ame.auralithpioneerinitiative.Mixin.Rendering;

import cute.ame.auralithpioneerinitiative.Seamless.SeamlessTransitionState;
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
    private void auralith$suppressDuringSeamlessTransition(Screen screen, CallbackInfo ci)
    {
        if (!SeamlessTransitionState.isTransitioning()) return;
        if (screen instanceof ReceivingLevelScreen) ci.cancel();
    }
}