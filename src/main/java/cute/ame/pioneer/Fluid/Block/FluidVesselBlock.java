package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Helper.VesselNodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FluidVesselBlock extends Block implements EntityBlock
{
    protected FluidVesselBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    public abstract float getVolumeLitres();

    public abstract boolean merges();

    // value between (0, 1]
    public abstract float getConductance();

    public abstract float getNominalBurstPressure();

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new FluidVesselBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            VesselNodes.onRemoved(level, pos, state);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected static BlockBehaviour.Properties metal(float hardness)
    {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(hardness, 6.0f).sound(SoundType.COPPER);
    }
}
