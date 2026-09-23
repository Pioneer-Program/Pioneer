package cute.ame.pioneer.Fluid.Block;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PioneerVesselBlock extends FluidVesselBlock
{
    protected PioneerVesselBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new PioneerVesselBlockEntity(pos, state);
    }

    protected static BlockBehaviour.Properties metal(float hardness)
    {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(hardness, 6.0f).sound(SoundType.COPPER);
    }
}
