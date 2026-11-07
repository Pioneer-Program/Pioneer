package cute.ame.pioneer.Data;

import cute.ame.pioneer.Fluid.Block.PipeBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

public class PipeBlockModel {

    public static void register(BlockStateProvider provider, Block block, ResourceLocation connectedTexture, ResourceLocation pipeTexture) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath();
        MultiPartBlockStateBuilder multipart = provider.getMultipartBuilder(block);

        //Connection arms
        for (Direction dir : Direction.values()) {
            multipart.part()
                .modelFile(new ModelFile.UncheckedModelFile(provider.modLoc("block/" + blockName + "/connection/" + dir.getSerializedName())))
                .addModel()
                .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), PipeBlock.PipeConnection.PIPE, PipeBlock.PipeConnection.RIM)
                .end();

            // Rims
            multipart.part()
                .modelFile(new ModelFile.UncheckedModelFile(provider.modLoc("block/" + blockName + "/rim/" + dir.getSerializedName())))
                .addModel()
                .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), PipeBlock.PipeConnection.RIM)
                .end();
        }

        // Core faces
        for (Direction face : Direction.values()) {
            Direction top = getTop(face);
            Direction bottom = getBottom(face);
            Direction left = getLeft(face);
            Direction right = getRight(face);

            for (int mask = 0; mask < 16; mask++) {
                boolean t = (mask & 1) != 0;
                boolean b = (mask & 2) != 0;
                boolean l = (mask & 4) != 0;
                boolean r = (mask & 8) != 0;

                ModelFile faceModel = getOrCreateCoreFaceModel(provider, blockName, connectedTexture, pipeTexture, face, mask, t, b, l, r);

                var part = multipart.part()
                    .modelFile(faceModel)
                    .addModel()
                    .condition(PipeBlock.PROPERTY_BY_DIRECTION.get(face), PipeBlock.PipeConnection.NONE);

                if (t) {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(top), PipeBlock.PipeConnection.PIPE, PipeBlock.PipeConnection.RIM);
                } else {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(top), PipeBlock.PipeConnection.NONE);
                }
                if (b) {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(bottom), PipeBlock.PipeConnection.PIPE, PipeBlock.PipeConnection.RIM);
                } else {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(bottom), PipeBlock.PipeConnection.NONE);
                }
                if (l) {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(left), PipeBlock.PipeConnection.PIPE, PipeBlock.PipeConnection.RIM);
                } else {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(left), PipeBlock.PipeConnection.NONE);
                }
                if (r) {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(right), PipeBlock.PipeConnection.PIPE, PipeBlock.PipeConnection.RIM);
                } else {
                    part.condition(PipeBlock.PROPERTY_BY_DIRECTION.get(right), PipeBlock.PipeConnection.NONE);
                }

                part.end();
            }
        }
    }

    private static ModelFile getOrCreateCoreFaceModel(BlockStateProvider provider, String blockName,
                                                      ResourceLocation connectedTexture, ResourceLocation pipeTexture,
                                                      Direction face, int mask,
                                                      boolean t, boolean b, boolean l, boolean r) {
        String modelName = "block/" + blockName + "/core/" + face.getSerializedName() + "_" + mask;

        int[] colRow = getAtlasCoordinates(t, b, l, r);
        int col = colRow[0];
        int row = colRow[1];

        float uMin = col * 4.0f + 0.5f;
        float uMax = col * 4.0f + 3.5f;
        float vMin = row * 4.0f + 0.5f;
        float vMax = row * 4.0f + 3.5f;

        BlockModelBuilder builder = provider.models().getBuilder(modelName)
            .texture("0", connectedTexture)
            .texture("particle", pipeTexture);

        var element = builder.element()
            .from(5, 5, 5)
            .to(11, 11, 11);

        var faceBuilder = element.face(face).texture("#0");
        if (face == Direction.UP || face == Direction.DOWN) {
            faceBuilder.uvs(uMin, vMin, uMax, vMax);
        } else {
            faceBuilder.uvs(uMax, vMin, uMin, vMax);
        }

        faceBuilder.end();
        element.end();

        return builder;
    }

    public static int[] getAtlasCoordinates(boolean t, boolean b, boolean l, boolean r) {
        if (!t && !b && !l && !r) return new int[]{0, 3}; // (Isolated)
        if (t && !b && !l && !r)  return new int[]{1, 1}; // (Top only)
        if (!t && b && !l && !r)  return new int[]{0, 0}; // (Bottom only)
        if (!t && !b && l && !r)  return new int[]{1, 0}; // (Left only)
        if (!t && !b && !l && r)  return new int[]{0, 1}; // (Right only)
        if (t && b && !l && !r)   return new int[]{0, 2}; // (Top + Bottom / Vertical)
        if (!t && !b && l && r)   return new int[]{1, 2}; // (Left + Right / Horizontal)
        if (t && !b && !l && r)   return new int[]{2, 1}; // (Top + Right)
        if (t && !b && l && !r)   return new int[]{3, 1}; // (Top + Left)
        if (!t && b && !l && r)   return new int[]{2, 0}; // (Bottom + Right)
        if (!t && b && l && !r)   return new int[]{3, 0}; // (Bottom + Left)
        if (t && b && !l && r)    return new int[]{2, 3}; // (Top + Bottom + Right)
        if (t && b && l && !r)    return new int[]{3, 2}; // (Top + Bottom + Left)
        if (t && !b && l && r)    return new int[]{3, 3}; // (Top + Left + Right)
        if (!t && b && l && r)    return new int[]{2, 2}; // (Bottom + Left + Right)
        return new int[]{1, 3};                           // (Cross)
    }

    public static Direction getTop(Direction face) {
        return switch (face) {
            case NORTH, SOUTH, EAST, WEST -> Direction.UP;
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
        };
    }

    public static Direction getBottom(Direction face) {
        return switch (face) {
            case NORTH, SOUTH, EAST, WEST -> Direction.DOWN;
            case UP -> Direction.SOUTH;
            case DOWN -> Direction.NORTH;
        };
    }

    public static Direction getLeft(Direction face) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            case UP -> Direction.WEST;
            case DOWN -> Direction.WEST;
        };
    }

    public static Direction getRight(Direction face) {
        return switch (face) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            case UP -> Direction.EAST;
            case DOWN -> Direction.EAST;
        };
    }
}
