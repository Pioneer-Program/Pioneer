package cute.ame.pioneer.Computer.BlockEntity;

import cute.ame.pioneer.Computer.Logic.Variable;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;

public class ComputerCoreEntity extends BlockEntity {

    private HashMap<ResourceLocation, Variable<?>> variables;

    public ComputerCoreEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMPUTER_CORE.get(), pos, blockState);
    }
}
