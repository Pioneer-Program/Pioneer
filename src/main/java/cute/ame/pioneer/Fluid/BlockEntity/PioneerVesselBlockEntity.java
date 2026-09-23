package cute.ame.pioneer.Fluid.BlockEntity;

import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import cute.ame.pioneer.Fluid.Block.PumpBlock;
import cute.ame.pioneer.Fluid.Block.ValveBlock;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PioneerVesselBlockEntity extends FluidVesselBlockEntity implements ComputerPeripheral
{
    private static final String K_LABEL = "label";
    private static final String K_POWER = "power";
    private static final String K_THROTTLE = "throttle";

    private String label = "";
    private float throttle = 1.0f;
    private float power = 1.0f;

    public PioneerVesselBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.FLUID_VESSEL.get(), pos, state);
    }

    public float getThrottle()
    {
        return throttle;
    }

    public boolean setThrottle(float value)
    {
        float clamped = !Float.isFinite(value) ? 1.0f : Math.clamp(value, 0.0f, 1.0f);
        if (clamped == throttle) return false;

        throttle = clamped;
        setChanged();

        if (level instanceof ServerLevel serverLevel)
        {
            FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
            if (data != null) data.graph().invalidate();
        }

        return true;
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
            BlockState state = getBlockState();
            FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
            if (data != null && state.getBlock() instanceof FluidVesselBlock vessel) data.graph().setOutletBoost(worldPosition, vessel.boost(serverLevel, worldPosition, state));
        }

        return true;
    }

    @Override
    public String getLabel()
    {
        return label;
    }

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
            if (ValveBlock.CHANNEL_THROTTLE.equalsIgnoreCase(channel)) return setThrottle((float) value);

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

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.loadAdditional(tag, registries);
        label = tag.getString(K_LABEL);
        power = tag.contains(K_POWER) ? tag.getFloat(K_POWER) : 1.0f;
        throttle = tag.contains(K_THROTTLE) ? tag.getFloat(K_THROTTLE) : 1.0f;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);

        if (!label.isEmpty()) tag.putString(K_LABEL, label);
        if (power != 1.0f) tag.putFloat(K_POWER, power);
        if (throttle != 1.0f) tag.putFloat(K_THROTTLE, throttle);
    }
}
