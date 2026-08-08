package cute.ame.auralithpioneerinitiative.Planet.Arid.Block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AridStrata extends Block
{
  public enum Variant
  {
    RED (MapColor.COLOR_RED),
    ORANGE (MapColor.COLOR_ORANGE),
    WHITE (MapColor.QUARTZ),
    MAROON (MapColor.TERRACOTTA_PURPLE);

    final MapColor mapColor;
    Variant(MapColor mapColor) { this.mapColor = mapColor; }
  }

  private final Variant variant;

  public AridStrata(Variant variant)
  {
    super(BlockBehaviour.Properties.of()
        .mapColor(variant.mapColor)
        .requiresCorrectToolForDrops()
        .strength(1.5f, 6.0f)
        .sound(SoundType.STONE));

    this.variant = variant;
  }

  public Variant getVariant() { return variant; }
}