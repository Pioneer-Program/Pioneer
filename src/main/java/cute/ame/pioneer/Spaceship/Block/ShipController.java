package cute.ame.pioneer.Spaceship.Block;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Spaceship.Entity.ShipEntity;
import cute.ame.pioneer.Spaceship.Helper.SableHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.command.SableAssembleCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ShipController extends Block implements EntityBlock {

    public static final Set<Block> DEFAULT_BLACKLIST = Set.of(
            Blocks.BEDROCK
    );

    public ShipController() {
        super(BlockBehaviour.Properties.of()
            .strength(1.0F, 1.0F)
        );
    }


    private InteractionResult assemble(ServerLevel level, BlockPos pos, ShipEntity controllerEntity) {
        SubLevelAssemblyHelper.GatherResult result = SableHelper.gatherConnectedBlocks(pos, level, SableAssembleCommands.DEFAULT_CONNECTED_ASSEMBLY_CAPACITY, DEFAULT_BLACKLIST);
        if (result.blocks() == null) {
            Pioneer.LOGGER.warn("Could not assemble ship controller!");
            return InteractionResult.FAIL;
        }
        controllerEntity.assemble(pos, result.blocks(), result.boundingBox());
        return InteractionResult.SUCCESS;
    }

    private InteractionResult disassemble(ShipEntity controllerEntity) {
        controllerEntity.disassemble();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        ShipEntity controllerEntity = (ShipEntity) level.getBlockEntity(pos);
        if (controllerEntity == null)
            return InteractionResult.FAIL;
        if (!controllerEntity.isAssemble())
            return assemble((ServerLevel) level, pos, controllerEntity);
        else
            return disassemble(controllerEntity);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ShipEntity(blockPos, blockState);
    }
}
