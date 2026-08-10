package cute.ame.pioneer.Seamless.Client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.ArrayList;
import java.util.List;

public final class SeamlessGhostSectionMesher
{
    private static final Direction[] DIRECTIONS = Direction.values();

    public static MeshedSection mesh(ClientLevel ghost, BlockPos sectionOrigin, int minY)
    {
        List<Quad> quads = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                int wx = sectionOrigin.getX() + x, wz = sectionOrigin.getZ() + z;
                int columnTop;
                try
                {
                    columnTop = ghost.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                }
                catch (Exception e)
                {
                    columnTop = Integer.MAX_VALUE;
                }
                int columnMinY = Math.min(minY, columnTop);

                for (int y = 0; y < 16; y++)
                {
                    int worldY = sectionOrigin.getY() + y;
                    if (worldY < columnMinY) continue;

                    pos.set(wx, worldY, wz);
                    BlockState state = ghost.getBlockState(pos);
                    if (state.isAir()) continue;

                    TextureAtlasSprite sprite;
                    var fluidState = state.getFluidState();
                    if (!fluidState.isEmpty())
                    {
                        ResourceLocation stillTexture = IClientFluidTypeExtensions.of(fluidState.getType()).getStillTexture(fluidState, ghost, pos);
                        sprite = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(stillTexture);
                    }
                    else
                    {
                        try
                        {
                            sprite = mc.getBlockRenderer().getBlockModel(state).getParticleIcon();
                        }
                        catch (Exception e)
                        {
                            continue;
                        }
                    }
                    if (sprite == null) continue;

                    int tint;
                    try
                    {
                        tint = mc.getBlockColors().getColor(state, ghost, pos, 0);
                    }
                    catch (Exception e)
                    {
                        tint = -1;
                    }
                    if (tint == -1) tint = 0xFFFFFF;

                    for (Direction dir : DIRECTIONS)
                    {
                        boolean visible;
                        if (dir == Direction.DOWN && worldY == columnMinY) visible = true;
                        else
                        {
                            neighborPos.setWithOffset(pos, dir);
                            boolean neighborChunkLoaded = ghost.hasChunk(neighborPos.getX() >> 4, neighborPos.getZ() >> 4);
                            if (!neighborChunkLoaded) visible = false;
                            else
                            {
                                BlockState neighbor = ghost.getBlockState(neighborPos);
                                visible = isFaceVisible(neighbor);
                            }
                        }
                        if (visible)
                        {
                            try
                            {
                                quads.add(buildQuad(ghost, pos, dir, sprite, tint));
                            }
                            catch (Exception e)
                            {

                            }
                        }
                    }
                }
            }
        }

        return new MeshedSection(quads);
    }

    private static boolean isFaceVisible(BlockState neighbor)
    {
        return neighbor.isAir() || !neighbor.canOcclude();
    }

    private static Quad buildQuad(ClientLevel ghost, BlockPos pos, Direction dir, TextureAtlasSprite sprite, int tint)
    {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        float x0 = x, y0 = y, z0 = z, x1 = x + 1f, y1 = y + 1f, z1 = z + 1f;

        float[][] corners = switch (dir)
        {
            case DOWN -> new float[][] {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case UP -> new float[][] {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case NORTH -> new float[][] {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case SOUTH -> new float[][] {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case WEST -> new float[][] {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case EAST -> new float[][] {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
        };

        float shade = switch (dir)
        {
            case UP -> 1.0f;
            case DOWN -> 0.5f;
            case NORTH, SOUTH -> 0.8f;
            case EAST, WEST -> 0.6f;
        };

        int packedLight = LevelRenderer.getLightColor(ghost, pos.relative(dir));

        return new Quad(corners, dir, shade, packedLight, tint, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
    }

    public record Quad(float[][] corners, Direction normal, float shade, int packedLight, int tint, float u0, float v0, float u1, float v1) {}

    public static final class MeshedSection
    {
        public final List<Quad> quads;

        public MeshedSection(List<Quad> quads)
        {
            this.quads = quads;
        }
    }
}