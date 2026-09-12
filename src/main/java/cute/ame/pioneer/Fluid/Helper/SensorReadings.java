package cute.ame.pioneer.Fluid.Helper;

import cute.ame.pioneer.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Level.RoomLevelData;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;
import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public final class SensorReadings
{
    public static final double UNREADABLE = Double.NaN;

    public static boolean isReadable(double value)
    {
        return !Double.isNaN(value);
    }

    public static int nodeAt(ServerLevel level, BlockPos pos)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return FluidNodeStore.INVALID;

        FluidNodeStore store = data.store();
        RoomLevelData rooms = RoomLevelData.getIfPresent(level);

        int room = FluidNodeStore.INVALID;

        for (Direction direction : Direction.values())
        {
            BlockPos side = pos.relative(direction);

            if (level.getBlockEntity(side) instanceof FluidVesselBlockEntity vessel)
            {
                int node = store.resolve(vessel.getNodeHandle());
                if (node != FluidNodeStore.INVALID) return node;
            }

            if (room == FluidNodeStore.INVALID && rooms != null)
            {
                int candidate = rooms.nodeAt(side);
                if (candidate != FluidNodeStore.INVALID && store.alive(candidate)) room = candidate;
            }
        }

        return room;
    }

    public static double pressure(ServerLevel level, BlockPos pos)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return UNREADABLE;

        int node = nodeAt(level, pos);
        if (node == FluidNodeStore.INVALID) return UNREADABLE;

        return data.store().pressure(node);
    }

    public static double temperature(ServerLevel level, BlockPos pos)
    {
        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return UNREADABLE;

        int node = nodeAt(level, pos);
        if (node == FluidNodeStore.INVALID) return UNREADABLE;

        return FluidConstants.toCelsius(data.store().temperature(node));
    }

    public static double gas(ServerLevel level, BlockPos pos, String species)
    {
        if (species == null || species.isEmpty()) return UNREADABLE;

        FluidLevelData data = FluidLevelData.getIfPresent(level);
        if (data == null) return UNREADABLE;

        SpeciesTable table = FluidSpecies.active();
        int index = table.indexOf(species);
        if (!table.isValid(index)) return UNREADABLE;

        int node = nodeAt(level, pos);
        if (node == FluidNodeStore.INVALID) return UNREADABLE;

        return data.store().fraction(node, index) * 100.0;
    }

    public static float dial(double value, double min, double max)
    {
        if (!isReadable(value) || max <= min) return 0.0f;

        double normalised = (value - min) / (max - min);
        if (normalised <= 0.0) return 0.0f;
        if (normalised >= 1.0) return 1.0f;

        return (float) normalised;
    }
}
