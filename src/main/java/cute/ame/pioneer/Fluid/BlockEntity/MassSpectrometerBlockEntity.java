package cute.ame.pioneer.Fluid.BlockEntity;

import cute.ame.celsius.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.celsius.Fluid.Data.FluidNodeStore;
import cute.ame.celsius.Fluid.Data.SpeciesTable;
import cute.ame.celsius.Fluid.Level.FluidLevelData;
import cute.ame.celsius.Fluid.Level.RoomLevelData;
import cute.ame.celsius.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Fluid.Block.SensorBlock;
import cute.ame.pioneer.Registrie.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class MassSpectrometerBlockEntity extends PioneerVesselBlockEntity
{
    private static final String K_LABEL = "label";
    private static final String K_GAS = "gas";

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float SAMPLE_MIN_MOL = 1.0e-6f;

    private String label = "";
    private String gas = SensorBlock.DEFAULT_GAS;
    private double reading = UNREADABLE;

    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    public MassSpectrometerBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.MASS_SPECTROMETER.get(), pos, state);
    }

    public String getGas()
    {
        return gas;
    }

    public void setGas(String key)
    {
        String next = key == null || key.isEmpty() ? SensorBlock.DEFAULT_GAS : key.toLowerCase(Locale.ROOT);
        if (next.equals(gas)) return;

        gas = next;
        setChanged();
    }

    public double reading()
    {
        return reading;
    }

    public void serverTick()
    {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameTime() % Math.max(Config.SENSOR_PERIOD_TICKS.get(), 1) != 0) return;

        FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
        int node = data == null ? FluidNodeStore.INVALID : sampleNode(serverLevel, data.store());

        if (node == FluidNodeStore.INVALID)
        {
            reading = UNREADABLE;
            return;
        }

        SpeciesTable table = FluidSpecies.active();
        int selected = table.indexOf(gas);
        reading = table.isValid(selected) ? data.store().fraction(node, selected) * 100.0 : UNREADABLE;
    }

    private int sampleNode(ServerLevel serverLevel, FluidNodeStore store)
    {
        int own = store.resolve(getNodeHandle());
        if (own != FluidNodeStore.INVALID && store.moles(own) > SAMPLE_MIN_MOL) return own;

        RoomLevelData rooms = RoomLevelData.getIfPresent(serverLevel);
        if (rooms == null) return FluidNodeStore.INVALID;

        for (Direction direction : DIRECTIONS)
        {
            int room = rooms.nodeAt(probe.setWithOffset(worldPosition, direction));
            if (room != FluidNodeStore.INVALID && store.alive(room)) return room;
        }
        return FluidNodeStore.INVALID;
    }

    public Component describe()
    {
        Component value = ComputerPeripheral.isReadable(reading) ? Component.translatable("gauge.pioneer.gas", String.format("%.2f", reading)) : Component.translatable("gauge.pioneer.no_reading");
        value = Component.translatable("gauge.pioneer.gas_of", Component.translatable("gas.pioneer." + gas), value);

        return label.isEmpty() ? value : Component.translatable("gauge.pioneer.labelled", label, value);
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
        if (!GAS_READING.equals(type)) return UNREADABLE;
        if (argument == null || argument.isEmpty() || argument.equalsIgnoreCase(gas)) return reading;
        if (!(level instanceof ServerLevel serverLevel)) return UNREADABLE;

        FluidLevelData data = FluidLevelData.getIfPresent(serverLevel);
        if (data == null) return UNREADABLE;

        int node = sampleNode(serverLevel, data.store());
        int species = FluidSpecies.active().indexOf(argument.toLowerCase(Locale.ROOT));
        if (node == FluidNodeStore.INVALID || !FluidSpecies.active().isValid(species)) return UNREADABLE;

        return data.store().fraction(node, species) * 100.0;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.loadAdditional(tag, registries);
        label = tag.getString(K_LABEL);
        gas = tag.contains(K_GAS) ? tag.getString(K_GAS) : SensorBlock.DEFAULT_GAS;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        super.saveAdditional(tag, registries);
        if (!label.isEmpty()) tag.putString(K_LABEL, label);
        tag.putString(K_GAS, gas);
    }
}
