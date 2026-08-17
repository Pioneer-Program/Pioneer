package cute.ame.pioneer.Mixin.Chunk;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Unique
    private static final double pionner$MAX_SPEED_PER_TICK = 500;

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "updateChunkScheduling", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;<init>(Lnet/minecraft/world/level/ChunkPos;ILnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/lighting/LevelLightEngine;Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;Lnet/minecraft/server/level/ChunkHolder$PlayerProvider;)V"), cancellable = true)
    private void debugChunk(long chunkPos, int newLevel, ChunkHolder holder, int oldLevel, CallbackInfoReturnable<ChunkHolder> cir) {
        if (!PioneerAPI.isSpaceDimension(this.level.dimension()))
            return;
        if (holder == null && PioneerAPI.ChunkLoadState.isAnyPlayerFast(this.level))
            cir.setReturnValue(null);
        else
            Pioneer.LOGGER.info("Creating chunk holder {}", new ChunkPos(chunkPos));
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void pioneer$preventSpaceChunkWriting(ChunkAccess chunk, CallbackInfoReturnable<Boolean> cir) {
        if (!PioneerAPI.isSpaceDimension(this.level.dimension()))
            return;
        boolean isOnlyAir = true;
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) {
                isOnlyAir = false;
                break;
            }
        }
        if (isOnlyAir)
            cir.setReturnValue(false);
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void pionner$preventSpaceChunkLoading(ServerPlayer player, CallbackInfo ci) {
        double speed = PioneerAPI.ChunkLoadState.getSpeed(player);
        Pioneer.LOGGER.info("Speed = " + speed);
        if (speed > pionner$MAX_SPEED_PER_TICK) {
            ci.cancel();
        }
    }
}
