package cute.ame.pioneer.Thermal.BlockEntity;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.celsius.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import cute.ame.pioneer.Thermal.Block.ThermalActuatorBlock;
import cute.ame.celsius.Thermal.Data.ThermalDevice;
import cute.ame.celsius.Thermal.Helper.ThermalActuator;
import cute.ame.celsius.Thermal.Level.ThermalLevelData;
import cute.ame.celsius.Thermal.Registry.ThermalDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThermalActuatorBlockEntity extends BlockEntity implements ComputerPeripheral
{
    private static final String K_LABEL = "label";
    private static final String K_SETPOINT = "setpoint";

    private String label = "";
    private float setpoint = Float.NaN;

    public ThermalActuatorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.THERMAL_ACTUATOR.get(), pos, state);
    }

    public float getSetpoint()
    {
        ThermalDevice device = ThermalDevices.of(getBlockState());
        if (device == null) return Float.isNaN(setpoint) ? 293.15f : setpoint;

        return Float.isNaN(setpoint) ? device.defaultSetpointK() : device.clampSetpoint(setpoint);
    }

    public void setSetpoint(float kelvin)
    {
        ThermalDevice device = ThermalDevices.of(getBlockState());
        setpoint = device == null ? kelvin : device.clampSetpoint(kelvin);
        setChanged();
    }

    public void serverTick()
    {
        if (!(level instanceof ServerLevel server)) return;

        int period = Math.max(cute.ame.celsius.Config.THERMAL_PERIOD.get(), 1);
        if (server.getGameTime() % period != 0) return;

        BlockState state = getBlockState();
        if (!ThermalActuatorBlock.isActive(state)) return;

        ThermalDevice device = ThermalDevices.of(state);
        if (device == null) return;

        float dt = (float) (period / 20.0 * cute.ame.celsius.Config.THERMAL_TIME_SCALE.get());
        float budget = device.watts() * dt;

        if (device.cooling()) ThermalActuator.pump(server, worldPosition, ThermalActuator.adjacentNode(server, worldPosition), getSetpoint(), budget);
        else ThermalActuator.heat(server, worldPosition, state, getSetpoint(), budget);
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
    public double read(ResourceLocation type, @Nullable String argument)
    {
        if (level == null) return UNREADABLE;

        if (TEMPERATURE_READING.equals(type))
        {
            if (!(level instanceof ServerLevel server)) return UNREADABLE;

            ThermalLevelData data = ThermalLevelData.getIfPresent(server);
            float kelvin = data == null ? Float.NaN : data.kelvinAt(worldPosition);

            return Float.isNaN(kelvin) ? BlockTemperature.dimensionDefault(server) : kelvin;
        }

        if (POWER_READING.equals(type))
        {
            ThermalDevice device = ThermalDevices.of(getBlockState());
            if (device == null) return UNREADABLE;

            return ThermalActuatorBlock.isActive(getBlockState()) ? device.watts() : 0.0;
        }

        return UNREADABLE;
    }

    @Override
    public boolean write(@Nullable String channel, double value)
    {
        if (level == null || level.isClientSide) return false;

        if (matches(channel, ThermalActuatorBlock.CHANNEL_SETPOINT))
        {
            setSetpoint((float) value);
            return true;
        }

        if (ThermalActuatorBlock.CHANNEL_ACTIVE.equalsIgnoreCase(channel))
        {
            ThermalActuatorBlock.setActive(level, worldPosition, getBlockState(), value >= 0.5);
            return true;
        }

        return false;
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
        setpoint = tag.contains(K_SETPOINT) ? tag.getFloat(K_SETPOINT) : Float.NaN;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);

        tag.putString(K_LABEL, label);
        if (!Float.isNaN(setpoint)) tag.putFloat(K_SETPOINT, setpoint);
    }
}
