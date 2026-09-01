package cute.ame.pioneer.Fluid.BlockEntity;

import cute.ame.pioneer.Fluid.Block.FluidVesselBlock;
import cute.ame.pioneer.Fluid.Block.ValveBlock;
import cute.ame.pioneer.Fluid.Helper.VesselNodes;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Physics.BurstRule;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidVesselBlockEntity extends BlockEntity implements ComputerPeripheral
{
    private static final String K_LABEL = "label";
    private String label = "";

    private static final String K_NODE = "node";
    private static final String K_BURST = "burst";

    private long node = -1L;
    private float burst;

    public FluidVesselBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.FLUID_VESSEL.get(), pos, state);
    }

    public long getNodeHandle() { return node; }

    @Override
    public String getLabel() { return label; }

    @Override
    public void setLabel(String label)
    {
        this.label = label == null ? "" : label;
        setChanged();
    }

    @Override
    public boolean write(@Nullable String channel, double value)
    {
        if (level == null || level.isClientSide) return false;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ValveBlock)) return false;
        if (channel != null && !channel.isEmpty() && !ValveBlock.CHANNEL_OPEN.equalsIgnoreCase(channel)) return false;

        ValveBlock.setOpen(level, worldPosition, state, value >= 0.5);
        return true;
    }

    public void setNodeHandle(long handle)
    {
        this.node = handle;
        setChanged();
    }

    public float getBurstPressure()
    {
        return burst;
    }

    public void setBurstPressure(float pressure)
    {
        this.burst = pressure;
        setChanged();
    }

    public void rollBurstPressure(ServerLevel level)
    {
        if (burst > 0.0f) return;
        if (!(getBlockState().getBlock() instanceof FluidVesselBlock vessel)) return;

        float amplitude = Config.BURST_JITTER.get().floatValue();
        setBurstPressure(BurstRule.jitter(vessel.getNominalBurstPressure(), amplitude, level.getRandom().nextFloat()));
    }

    @Override
    public void onLoad()
    {
        super.onLoad();

        if (level instanceof ServerLevel serverLevel)
        {
            rollBurstPressure(serverLevel);
            VesselNodes.onLoaded(serverLevel, worldPosition, this);
        }
    }

    @Override
    public void setRemoved()
    {
        if (level instanceof ServerLevel serverLevel) VesselNodes.onUnloaded(serverLevel, worldPosition);

        super.setRemoved();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.loadAdditional(tag, registries);
        node = tag.getLong(K_NODE);
        label = tag.getString(K_LABEL);
        burst = tag.getFloat(K_BURST);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putLong(K_NODE, node);
        tag.putString(K_LABEL, label);
        tag.putFloat(K_BURST, burst);
    }
}
