package cute.ame.pioneer.Thermal.BlockEntity;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Core.Thermal.BlockHeatSink;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Physics.FluidHeat;
import cute.ame.pioneer.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import cute.ame.pioneer.Thermal.Block.ThermalActuatorBlock;
import cute.ame.pioneer.Thermal.Data.MaterialTable;
import cute.ame.pioneer.Thermal.Data.ThermalDevice;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import cute.ame.pioneer.Thermal.Registry.ThermalDevices;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

        int period = Math.max(Config.THERMAL_PERIOD.get(), 1);
        if (server.getGameTime() % period != 0) return;

        BlockState state = getBlockState();
        if (!ThermalActuatorBlock.isActive(state)) return;

        ThermalDevice device = ThermalDevices.of(state);
        if (device == null) return;

        float dt = (float) (period / 20.0 * Config.THERMAL_TIME_SCALE.get());
        float budget = device.watts() * dt;

        if (device.cooling()) pump(server, device, budget);
        else heat(server, state, device, budget);
    }

    private void heat(ServerLevel server, BlockState state, ThermalDevice device, float budget)
    {
        float capacity = capacityOf(state);
        if (capacity <= 0.0f) return;

        ThermalLevelData data = ThermalLevelData.get(server);

        float current = data.kelvinAt(worldPosition);
        if (Float.isNaN(current)) current = BlockTemperature.dimensionDefault(server);

        float owed = (getSetpoint() - current) * capacity;
        if (owed <= 0.0f) return;

        float change = Math.min(owed, budget) / capacity;
        if (change < Config.THERMAL_EPSILON_K.get()) return;

        data.force(worldPosition, current + change);
    }

    private void pump(ServerLevel server, ThermalDevice device, float budget)
    {
        FluidLevelData fluids = FluidLevelData.getIfPresent(server);
        if (fluids == null) return;

        FluidNodeStore store = fluids.store();
        int node = adjacentNode(server, store);
        if (node == FluidNodeStore.INVALID) return;

        float[] molarHeat = FluidSpecies.active().molarHeatRaw();
        double fluidCapacity = FluidHeat.capacity(store, node, molarHeat);
        if (fluidCapacity <= 0.0) return;

        double owed = (getSetpoint() - store.temperature(node)) * fluidCapacity;
        if (owed >= 0.0) return;

        double joules = Math.max(owed, -budget);

        float moved = FluidHeat.addJoules(store, node, joules, molarHeat);
        if (moved == 0.0f) return;

        fluids.touch(node);

        double removed = -moved * fluidCapacity;
        if (removed <= 0.0) return;

        BlockHeatSink.inject(server, worldPosition, removed);
    }

    private int adjacentNode(ServerLevel server, FluidNodeStore store)
    {
        for (Direction face : Direction.values())
        {
            if (!(server.getBlockEntity(worldPosition.relative(face)) instanceof FluidVesselBlockEntity vessel)) continue;

            int node = store.resolve(vessel.getNodeHandle());
            if (node != FluidNodeStore.INVALID && store.moles(node) > 0.0f) return node;
        }

        return FluidNodeStore.INVALID;
    }

    private static float capacityOf(BlockState state)
    {
        MaterialTable table = ThermalMaterials.table();
        int material = ThermalMaterials.indexOf(state);

        return table.isValid(material) ? table.volumetricHeat(material) : 0.0f;
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
