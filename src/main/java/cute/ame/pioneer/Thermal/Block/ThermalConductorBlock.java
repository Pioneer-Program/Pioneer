package cute.ame.pioneer.Thermal.Block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ThermalConductorBlock extends Block
{
    public ThermalConductorBlock()
    {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops().sound(SoundType.COPPER));
    }
}
