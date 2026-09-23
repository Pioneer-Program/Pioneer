package cute.ame.pioneer.Mixin.World;

import cute.ame.pioneer.Fluid.Level.RoomLevelData;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class BlockChangeMixin
{
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void pioneer$onBlockChanged(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir)
    {
        BlockState previous = cir.getReturnValue();
        if (previous == null || previous == state) return;

        Level level = ((LevelChunk) (Object) this).getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        RoomLevelData rooms = RoomLevelData.getIfPresent(serverLevel);
        if (rooms != null) rooms.onBlockChanged(pos);

        if (previous.getBlock() != state.getBlock()) ThermalLevelData.onBlockReplaced(serverLevel, pos);
    }
}
