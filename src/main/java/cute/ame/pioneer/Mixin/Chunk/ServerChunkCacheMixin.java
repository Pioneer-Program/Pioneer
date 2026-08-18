package cute.ame.pioneer.Mixin.Chunk;

import com.llamalad7.mixinextras.sugar.Local;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {

    @Shadow
    @Final
    public ServerLevel level;

    @Shadow
    protected abstract boolean chunkAbsent(@Nullable ChunkHolder chunkHolder, int status);

    @Shadow
    @Final
    public ChunkMap chunkMap;


    @Inject(method = "getChunkFutureMainThread", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;pauseInIde(Ljava/lang/Throwable;)Ljava/lang/Throwable;", shift = At.Shift.BEFORE), cancellable = true)
    private void getChunkFutureMainThread(int x, int z, ChunkStatus chunkStatus, boolean requireChunk, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir) {
        if  (PioneerAPI.isSpaceDimension(level.dimension())) {
            cir.setReturnValue(GenerationChunkHolder.UNLOADED_CHUNK_FUTURE);
            cir.cancel();
        }
    }

    @Inject(method = "getChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;incrementCounter(Ljava/lang/String;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk, CallbackInfoReturnable<ChunkAccess> cir) {
        if  (PioneerAPI.isSpaceDimension(level.dimension()) && Pioneer.isPlayerLoadingTooFast(new ChunkPos(x, z))) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }
}
