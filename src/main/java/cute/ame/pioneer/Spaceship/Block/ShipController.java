package cute.ame.pioneer.Spaceship.Block;

import cute.ame.pioneer.Spaceship.Entity.ShipControllerEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.command.SableAssembleCommands;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ShipController extends Block implements EntityBlock {

    public ShipController() {
        super(BlockBehaviour.Properties.of()
            .strength(1.0F, 1.0F)
        );
    }


    private InteractionResult assemble(ServerLevel level, BlockPos pos, ShipControllerEntity controllerEntity) {
        SubLevelAssemblyHelper.GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(pos, level, SableAssembleCommands.DEFAULT_CONNECTED_ASSEMBLY_CAPACITY, null);
        controllerEntity.setAssemblyBlocks(result.blocks());
        controllerEntity.assemble(pos, result.blocks(), result.boundingBox());
        return InteractionResult.SUCCESS;
    }

    private InteractionResult disassemble(ShipControllerEntity controllerEntity) {
        controllerEntity.disassemble();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        ShipControllerEntity controllerEntity = (ShipControllerEntity) level.getBlockEntity(pos);
        if (controllerEntity == null)
            return InteractionResult.FAIL;
        System.out.println(controllerEntity.isAssemble());
        if (!controllerEntity.isAssemble())
            return assemble((ServerLevel) level, pos, controllerEntity);
        else
            return disassemble(controllerEntity);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ShipControllerEntity(blockPos, blockState);
    }
}
