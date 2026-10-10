package cute.ame.pioneer.Fluid.Block;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Core.Readout.FluidReadout;
import cute.ame.pioneer.Core.Readout.Readout;
import cute.ame.pioneer.Core.Readout.ReadoutSource;
import cute.ame.pioneer.Core.Readout.ReadoutUnit;
import cute.ame.pioneer.Fluid.BlockEntity.PioneerVesselBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PioneerVesselBlock extends FluidVesselBlock implements ReadoutSource
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

    @Override
    public void readout(ServerLevel level, BlockPos pos, BlockState state, Readout out)
    {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ComputerPeripheral peripheral && !peripheral.getLabel().isEmpty())
            out.text("readout.pioneer.label", peripheral.getLabel());

        details(level, pos, state, out);

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null || !(entity instanceof FluidVesselBlockEntity vessel)) return;

        FluidNodeStore store = data.store();
        int node = store.resolve(vessel.getNodeHandle());
        if (node == FluidNodeStore.INVALID) return;

        FluidReadout.node(level, pos, store, node, out);
        out.number("readout.pioneer.burst", getNominalBurstPressure(), ReadoutUnit.PRESSURE);
    }

    protected void details(ServerLevel level, BlockPos pos, BlockState state, Readout out)
    {
    }

    protected static BlockBehaviour.Properties metal(float hardness)
    {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(hardness, 6.0f).sound(SoundType.COPPER);
    }
}
