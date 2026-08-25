package cute.ame.pioneer.Spaceship.Data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record StoredBlock(BlockPos pos, BlockState state, CompoundTag tag) {

    public void place(Level level, Vec3i offset) {
        BlockPos calcPos = pos.offset(offset);
        level.setBlock(calcPos, state, Block.UPDATE_CLIENTS);
        BlockEntity newBlockEntity = level.getBlockEntity(calcPos);
        if (newBlockEntity != null && tag != null)
            newBlockEntity.loadWithComponents(tag, level.registryAccess());
    }

}
