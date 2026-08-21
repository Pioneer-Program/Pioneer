package cute.ame.pioneer.Mixin.World;

import cute.ame.pioneer.Fluid.Room.RoomLevelData;
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
public abstract class RoomInvalidationMixin
{
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void pioneer$invalidateRooms(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir)
    {
        BlockState previous = cir.getReturnValue();
        if (previous == null || previous == state) return;

        Level level = ((LevelChunk) (Object) this).getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        RoomLevelData rooms = RoomLevelData.getIfPresent(serverLevel);
        if (rooms == null) return;

        rooms.onBlockChanged(pos);
    }
}