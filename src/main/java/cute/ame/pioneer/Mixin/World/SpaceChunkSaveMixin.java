package cute.ame.pioneer.Mixin.World;

import cute.ame.pioneer.SkyPlanet.Dimension.SpaceChunkSaves;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class SpaceChunkSaveMixin
{
    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z", at = @At("HEAD"))
    private void pioneer$skipEmptySpaceChunk(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir)
    {
        SpaceChunkSaves.beforeSave(level, chunk);
    }
}
