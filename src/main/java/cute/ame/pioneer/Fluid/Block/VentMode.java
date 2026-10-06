package cute.ame.pioneer.Fluid.Block;

import cute.ame.celsius.Fluid.Block.FluidVesselBlock;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum VentMode implements StringRepresentable
{
    BIDIRECTIONAL("bidirectional", FluidVesselBlock.BRIDGE_BOTH),
    INTAKE("intake", FluidVesselBlock.BRIDGE_FROM_ROOM),
    EXHAUST("exhaust", FluidVesselBlock.BRIDGE_TO_ROOM);

    private static final VentMode[] VALUES = values();

    private final String name;
    private final int flow;

    VentMode(String name, int flow)
    {
        this.name = name;
        this.flow = flow;
    }

    public static VentMode byIndex(int index)
    {
        return VALUES[Math.clamp(index, 0, VALUES.length - 1)];
    }

    public int flow()
    {
        return flow;
    }

    public VentMode cycle(boolean back)
    {
        return VALUES[(ordinal() + (back ? VALUES.length - 1 : 1)) % VALUES.length];
    }

    @Override
    public @NotNull String getSerializedName()
    {
        return name;
    }
}
