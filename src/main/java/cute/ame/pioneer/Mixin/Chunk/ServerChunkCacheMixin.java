package cute.ame.pioneer.Mixin.Chunk;

import com.llamalad7.mixinextras.sugar.Local;
import cute.ame.pioneer.Core.API.PioneerAPI;
import net.minecraft.Util;
import net.minecraft.server.level.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {

    @Shadow
    protected abstract boolean chunkAbsent(@Nullable ChunkHolder chunkHolder, int status);

    @Shadow
    @Final
    public ChunkMap chunkMap;

    @Shadow
    @Final
    public ServerLevel level;

    @Shadow
    protected abstract void storeInCache(long chunkPos, @Nullable ChunkAccess chunk, ChunkStatus chunkStatus);

    @Inject(method = "getChunkFutureMainThread", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", shift = At.Shift.AFTER), cancellable = true)
    private void pionner$removeTickError(int x, int z, ChunkStatus chunkStatus, boolean requireChunk, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir, @Local(ordinal = 2) int j, @Local ChunkHolder chunkholder) {
        cir.setReturnValue(this.chunkAbsent(chunkholder, j) ? GenerationChunkHolder.UNLOADED_CHUNK_FUTURE : chunkholder.scheduleChunkGenerationTask(chunkStatus, this.chunkMap));
        cir.cancel();
    }

    @Inject(method = "getChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkResult;orElse(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.BEFORE), cancellable = true)
    private void pionner$removeChunkError(int x, int z, ChunkStatus chunkStatus, boolean requireChunk, CallbackInfoReturnable<ChunkAccess> cir, @Local long i, @Local ChunkResult<ChunkAccess> chunkresult) {
        ChunkAccess chunkaccess1 = (ChunkAccess)chunkresult.orElse(null);
        if (chunkaccess1 == null && requireChunk && !PioneerAPI.isSpaceDimension(level.dimension()))
            throw Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + chunkresult.getError()));
        else {
            this.storeInCache(i, chunkaccess1, chunkStatus);
            cir.setReturnValue(chunkaccess1);
            cir.cancel();
        }
    }

}
