package cute.ame.pioneer.Mixin.Chunk;

import com.llamalad7.mixinextras.sugar.Local;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Mixin.ChunkMapAccessor;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.*;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin {

    @Shadow
    protected abstract SortedArraySet<Ticket<?>> getTickets(long chunkPos);

    @Shadow
    @Final
    private Executor mainThreadExecutor;

    @Shadow
    @Final
    private ProcessorHandle<ChunkTaskPriorityQueueSorter.Release> ticketThrottlerReleaser;

    @Shadow
    @Final
    private LongSet ticketsToRelease;

    @Inject(method = "runAllUpdates", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongIterator;hasNext()Z", shift = At.Shift.BEFORE), cancellable = true)
    private void runAllUpdates(ChunkMap chunkMap, CallbackInfoReturnable<Boolean> cir, @Local LongIterator longiterator, @Local boolean flag) {
        while(longiterator.hasNext()) {
            long j = longiterator.nextLong();
            if (this.getTickets(j).stream().anyMatch((p_183910_) -> p_183910_.getType() == TicketType.PLAYER)) {
                ChunkHolder chunkholder = ((ChunkMapAccessor) chunkMap).pionner$getUpdatingChunkIfPresent(j);
                if (chunkholder == null) {
                    if (PioneerAPI.isSpaceDimension(((ChunkMapAccessor) chunkMap).getLevel().dimension()))
                        continue;
                    throw new IllegalStateException();
                }
                CompletableFuture<ChunkResult<LevelChunk>> completablefuture = chunkholder.getEntityTickingChunkFuture();
                completablefuture.thenAccept((p_331640_) -> this.mainThreadExecutor.execute(() -> this.ticketThrottlerReleaser.tell(ChunkTaskPriorityQueueSorter.release(() -> {
                }, j, false))));
            }
        }
        this.ticketsToRelease.clear();
        cir.setReturnValue(flag);
        cir.cancel();
    }

}
