package cute.ame.pioneer.Planet.Arid.Block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AridRock extends Block
{
  public enum Variant { RAW, CRACKED, POLISHED }

  private final Variant variant;

  public AridRock(Variant variant)
  {
    super(properties(variant));
    this.variant = variant;
  }

  public Variant getVariant() { return variant; }

  private static BlockBehaviour.Properties properties(Variant variant)
  {
    return BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_ORANGE)
        .requiresCorrectToolForDrops()
        .strength(variant == Variant.POLISHED ? 2.5f : 1.8f,
            variant == Variant.CRACKED  ? 4.0f : 6.0f)
        .sound(SoundType.STONE);
  }
}