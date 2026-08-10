package cute.ame.pioneer.Planet.Arid.Block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AridCrystal extends Block
{
  public enum Size
  {
    SMALL (0,  4, 6,  4),
    MEDIUM (8,  6, 10, 6),
    LARGE (12, 8, 14, 8);

    final int lightLevel;
    final int sizeX, sizeY, sizeZ;

    Size(int lightLevel, int sizeX, int sizeY, int sizeZ)
    {
      this.lightLevel = lightLevel;
      this.sizeX = sizeX;
      this.sizeY = sizeY;
      this.sizeZ = sizeZ;
    }
  }

  private final Size size;
  private final VoxelShape shape;

  public AridCrystal(Size size)
  {
    super(BlockBehaviour.Properties.of()
        .mapColor(MapColor.COLOR_CYAN)
        .requiresCorrectToolForDrops()
        .strength(1.0f, 2.0f)
        .sound(SoundType.AMETHYST)
        .lightLevel(state -> size.lightLevel)
        .noOcclusion());

    this.size = size;

    float ox = (16 - size.sizeX) / 2f;
    float oz = (16 - size.sizeZ) / 2f;
    this.shape = Block.box(ox, 0, oz, ox + size.sizeX, size.sizeY, oz + size.sizeZ);
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
  {
    return shape;
  }

  @Override
  public VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
  {
    return shape;
  }

  public Size getCrystalSize() { return size; }
}