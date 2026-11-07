package cute.ame.pioneer.Mixin.World;

import cute.ame.pioneer.LifeSupport.Level.KelpBeds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class KelpBedChangeMixin
{
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void pioneer$onKelpBedChanged(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir)
    {
        if (!KelpBeds.anyTracked()) return;

        BlockState previous = cir.getReturnValue();
        if (previous == null || previous == state) return;

        if (((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) KelpBeds.onBlockChanged(level, pos);
    }
}
