package cute.ame.pioneer.Planet.Arid.Block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AridGlyph extends Block
{
  public AridGlyph()
  {
    super(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_ORANGE)
        .requiresCorrectToolForDrops()
        .strength(2.0f, 8.0f)
        .sound(SoundType.STONE));
  }
}