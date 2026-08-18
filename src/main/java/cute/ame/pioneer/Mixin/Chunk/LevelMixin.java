package cute.ame.pioneer.Mixin.Chunk;

import com.llamalad7.mixinextras.sugar.Local;
import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin extends AttachmentHolder implements LevelAccessor, AutoCloseable, ILevelExtension {

    @Shadow
    @Final
    private ResourceKey<Level> dimension;

    @Shadow
    @Final
    public boolean isClientSide;

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk, CallbackInfoReturnable<ChunkAccess> cir) {

        if (PioneerAPI.isSpaceDimension(dimension)) {
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    @Inject(method = "getBlockState", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;", shift = At.Shift.AFTER), cancellable = true)
    private void getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir, @Local LevelChunk chunk) {
        if (chunk == null && PioneerAPI.isSpaceDimension(dimension)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
            cir.cancel();
        }
    }

    @Inject(method = "getFluidState", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getChunkAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/LevelChunk;", shift = At.Shift.AFTER), cancellable = true)
    private void getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir, @Local LevelChunk chunk) {
        if (chunk == null && PioneerAPI.isSpaceDimension(dimension)) {
            cir.setReturnValue(Fluids.EMPTY.defaultFluidState());
            cir.cancel();
        }
    }

}
