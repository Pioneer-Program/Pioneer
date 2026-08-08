package cute.ame.auralithpioneerinitiative.Mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftLevelRendererAccessor
{
    @Accessor("renderBuffers")
    RenderBuffers auralith$getRenderBuffers();

    @Accessor("level")
    ClientLevel auralith$getLevel();

    @Mutable
    @Accessor("level")
    void auralith$setLevel(ClientLevel level);
}