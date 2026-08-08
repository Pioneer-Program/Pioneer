package cute.ame.auralithpioneerinitiative.Planet.Arid.Block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AridDust extends FallingBlock
{
  private static final MapCodec<AridDust> CODEC = simpleCodec(p -> new AridDust());

  @Override
  public MapCodec<AridDust> codec() { return CODEC; }

  public AridDust()
  {
    super(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_ORANGE)
        .strength(0.4f)
        .sound(SoundType.SAND)
        .requiresCorrectToolForDrops());
  }
}