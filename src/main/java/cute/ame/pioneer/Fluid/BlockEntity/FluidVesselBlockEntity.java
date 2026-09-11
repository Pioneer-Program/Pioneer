package cute.ame.pioneer.Fluid.BlockEntity;

import cute.ame.pioneer.Fluid.Block.FluidVesselBlock;
import cute.ame.pioneer.Fluid.Block.PumpBlock;
import cute.ame.pioneer.Fluid.Block.ValveBlock;
import cute.ame.pioneer.Fluid.Helper.VesselNodes;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Physics.BurstRule;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
    private static final String K_POWER = "power";

    private long node = -1L;
    private float burst;
    private float power = 1.0f;

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

        if (state.getBlock() instanceof ValveBlock)
        {
            if (!matches(channel, ValveBlock.CHANNEL_OPEN)) return false;

            ValveBlock.setOpen(level, worldPosition, state, value >= 0.5);
            return true;
        }

        if (state.getBlock() instanceof PumpBlock)
        {
            if (matches(channel, PumpBlock.CHANNEL_POWER)) return setPower(value);

            if (PumpBlock.CHANNEL_RUNNING.equalsIgnoreCase(channel))
            {
                boolean run = value >= 0.5;
                if (run && power <= 0.0f) return setPower(1.0);

                PumpBlock.setActive(level, worldPosition, state, run);
                return true;
            }
        }

        return false;
    }

    @Override
    public double read(ResourceLocation type, @Nullable String argument)
    {
        if (!POWER_READING.equals(type)) return UNREADABLE;
        if (!(getBlockState().getBlock() instanceof PumpBlock)) return UNREADABLE;

        return PumpBlock.isActive(getBlockState()) ? power : 0.0;
    }

    private static boolean matches(@Nullable String channel, String expected)
    {
        return channel == null || channel.isEmpty() || expected.equalsIgnoreCase(channel);
    }

    public float getPower()
    {
        return power;
    }

    public boolean setPower(double value)
    {
        if (level == null || level.isClientSide) return false;
        if (!(getBlockState().getBlock() instanceof PumpBlock)) return false;

        float next = (float) Mth.clamp(value, 0.0, 1.0);
        boolean changed = next != power;

        if (changed)
        {
            power = next;
            setChanged();
        }

        PumpBlock.setActive(level, worldPosition, getBlockState(), next > 0.0f);

        if (changed && level instanceof ServerLevel serverLevel)
        {
            FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
            if (data != null) data.graph().setPumpBoost(worldPosition, (float) (Config.PUMP_BOOST_P.get() * next));
        }

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
        power = tag.contains(K_POWER) ? tag.getFloat(K_POWER) : 1.0f;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putLong(K_NODE, node);
        tag.putString(K_LABEL, label);
        tag.putFloat(K_BURST, burst);

        if (power != 1.0f) tag.putFloat(K_POWER, power);
    }
}
