package cute.ame.pioneer.Fluid.BlockEntity;

import cute.ame.pioneer.Fluid.Block.MassSpectrometerBlock;

import cute.ame.pioneer.Fluid.Block.SensorBlock;
import cute.ame.pioneer.Fluid.Helper.SensorReadings;

import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SensorBlockEntity extends BlockEntity implements ComputerPeripheral
{
    private static final String K_LABEL = "label";
    private static final String K_GAS = "gas";
    private static final String K_DIAL = "dial";
    private static final float DIAL_STEP = 1.0f / 256.0f;

    private String label = "";
    private String gas = SensorBlock.DEFAULT_GAS;

    private float dial;
    private double reading = SensorReadings.UNREADABLE;

    public SensorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SENSOR.get(), pos, state);
    }

    public SensorBlock instrument()
    {
        return getBlockState().getBlock() instanceof SensorBlock sensor ? sensor : null;
    }

    public float dial()
    {
        return dial;
    }

    public String getGas()
    {
        return gas;
    }

    public void setGas(String gas)
    {
        this.gas = (gas == null || gas.isEmpty()) ? SensorBlock.DEFAULT_GAS : gas.toLowerCase(java.util.Locale.ROOT);
        setChanged();
    }

    @Override
    public String getLabel() { return label; }

    @Override
    public void setLabel(String label)
    {
        this.label = label == null ? "" : label;
        setChanged();
    }

    @Override
    public double read(ResourceLocation type, @Nullable String argument)
    {
        if (!(level instanceof ServerLevel serverLevel)) return UNREADABLE;

        SensorBlock instrument = instrument();
        if (instrument == null || !instrument.getReadingType().equals(type)) return UNREADABLE;

        return instrument.readAt(serverLevel, worldPosition, argument == null || argument.isEmpty() ? gas : argument);
    }

    public void serverTick()
    {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (level.getGameTime() % Math.max(Config.SENSOR_PERIOD_TICKS.get(), 1) != 0) return;

        SensorBlock instrument = instrument();
        if (instrument == null) return;

        reading = instrument.readAt(serverLevel, worldPosition, gas);

        float next = SensorReadings.dial(reading, instrument.getDialMin(), instrument.getDialMax());
        if (Math.abs(next - dial) < DIAL_STEP) return;

        dial = next;
        setChanged();
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    public Component describe()
    {
        SensorBlock instrument = instrument();
        if (instrument == null) return Component.translatable("gauge.pioneer.no_reading");

        Component value = instrument.describeValue(reading);

        if (instrument instanceof MassSpectrometerBlock)
        {
            SpeciesTable table = FluidSpecies.active();
            String key = table.isValid(table.indexOf(gas)) ? gas : SensorBlock.DEFAULT_GAS;
            value = Component.translatable("gauge.pioneer.gas_of", Component.translatable("gas.pioneer." + key), value);
        }

        if (label.isEmpty()) return value;

        return Component.translatable("gauge.pioneer.labelled", label, value);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.loadAdditional(tag, registries);
        label = tag.getString(K_LABEL);
        gas = tag.contains(K_GAS) ? tag.getString(K_GAS) : SensorBlock.DEFAULT_GAS;
        dial = tag.getFloat(K_DIAL);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putString(K_LABEL, label);
        tag.putString(K_GAS, gas);
        tag.putFloat(K_DIAL, dial);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SensorBlockEntity sensor)
    {
        sensor.serverTick();
    }
}
