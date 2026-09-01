package cute.ame.pioneer.Fluid.Block;

import com.mojang.serialization.MapCodec;
import cute.ame.pioneer.Core.Computer.ComputerPeripheral;
import cute.ame.pioneer.Fluid.Helper.SensorReadings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import org.jetbrains.annotations.NotNull;

public class BarometerBlock extends SensorBlock
{
    public static final MapCodec<BarometerBlock> CODEC = simpleCodec(p -> new BarometerBlock());

    @Override
    public @NotNull ResourceLocation getReadingType()
    {
        return ComputerPeripheral.PRESSURE_READING;
    }

    @Override
    public double readAt(ServerLevel level, BlockPos pos, String argument)
    {
        return SensorReadings.pressure(level, pos);
    }

    @Override
    public double getDialMin()
    {
        return 0.0;
    }

    @Override
    public double getDialMax()
    {
        return 25.0;
    }

    @Override
    public @NotNull Component describeValue(double value)
    {
        if (!SensorReadings.isReadable(value)) return Component.translatable("gauge.pioneer.no_reading");

        return Component.translatable("gauge.pioneer.pressure", String.format("%.3f", value));
    }

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec()
    {
        return CODEC;
    }
}
