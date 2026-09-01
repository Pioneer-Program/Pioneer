package cute.ame.pioneer.Core.Computer;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface ComputerPeripheral
{
    ResourceLocation PRESSURE_READING = ResourceLocation.fromNamespaceAndPath("pioneer", "pressure_reading");
    ResourceLocation TEMPERATURE_READING = ResourceLocation.fromNamespaceAndPath("pioneer", "temperature_reading");
    ResourceLocation GAS_READING = ResourceLocation.fromNamespaceAndPath("pioneer", "gas_reading");

    double UNREADABLE = Double.NaN;

    static boolean isReadable(double value)
    {
        return !Double.isNaN(value);
    }

    // computer source and target fields are matched against this shit
    String getLabel();

    void setLabel(String label);

    default double read(ResourceLocation type, @Nullable String argument)
    {
        return UNREADABLE;
    }

    default boolean write(@Nullable String channel, double value)
    {
        return false;
    }

    default boolean write(@Nullable String channel, boolean value)
    {
        return write(channel, value ? 1.0 : 0.0);
    }
}
