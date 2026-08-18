package cute.ame.pioneer.Mixin.Chunk;

import com.llamalad7.mixinextras.sugar.Local;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {


    @Shadow
    @Final
    private ServerLevel level;

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

    @Inject(method = "updateChunkScheduling", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;<init>(Lnet/minecraft/world/level/ChunkPos;ILnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/lighting/LevelLightEngine;Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;Lnet/minecraft/server/level/ChunkHolder$PlayerProvider;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void pionner$preventSpaceChunkScheduling(long chunkPos, int newLevel, ChunkHolder holder, int oldLevel, CallbackInfoReturnable<ChunkHolder> cir) {
        if (PioneerAPI.isSpaceDimension(this.level.dimension()) && Pioneer.isPlayerLoadingTooFast(new ChunkPos(chunkPos))) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    @Inject(method = "acquireGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;increaseGenerationRefCount()V", shift = At.Shift.BEFORE), cancellable = true)
    private void pionner$acquireGeneration(long chunkPos, CallbackInfoReturnable<GenerationChunkHolder> cir, @Local ChunkHolder chunkholder) {
        if (chunkholder == null &&  PioneerAPI.isSpaceDimension(this.level.dimension())) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    @Inject(method = "releaseGeneration", at = @At("HEAD"), cancellable = true)
    private void pionner$releaseGeneration(GenerationChunkHolder chunk, CallbackInfo ci) {
        if (chunk == null &&  PioneerAPI.isSpaceDimension(this.level.dimension()))
            ci.cancel();
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void move(ServerPlayer player, CallbackInfo ci) {
        Pioneer.PLAYER_MAP.put(player.chunkPosition().toLong(), player);
    }
}
