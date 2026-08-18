package cute.ame.pioneer.Mixin.Chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkGenerationTask;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerationTask.class)
public abstract class ChunkGenerationTaskMixin {

    @Shadow
    public abstract void markForCancellation();

    @Inject(method = "scheduleChunkInLayer", at = @At("HEAD"), cancellable = true)
    private void scheduleChunkInLayer(ChunkStatus status, boolean needsGeneration, GenerationChunkHolder chunk, CallbackInfoReturnable<Boolean> cir) {
        if (chunk == null) {
            this.markForCancellation();
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
