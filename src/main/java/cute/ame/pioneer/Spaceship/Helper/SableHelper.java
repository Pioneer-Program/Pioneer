package cute.ame.pioneer.Spaceship.Helper;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class SableHelper {

    public static @NotNull SubLevelAssemblyHelper.GatherResult gatherConnectedBlocks(final BlockPos gatherOrigin, final ServerLevel level, final int maximumBlocksToAssemble, final Set<Block> blacklist) {
        final LinkedHashSet<Pair<BlockPos, BlockState>> frontier = new LinkedHashSet<>(1 << 12);
        final Set<BlockPos> blocks = new ObjectOpenHashSet<>(1 << 10);
        final LevelAccelerator accelerator = new LevelAccelerator(level);
        final BlockState gatherOriginState = accelerator.getBlockState(gatherOrigin);
        if (gatherOriginState.isAir())
            return new SubLevelAssemblyHelper.GatherResult(null, 0, null, SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS);
        frontier.add(Pair.of(gatherOrigin, gatherOriginState));
        int minX = gatherOrigin.getX(), minY = gatherOrigin.getY(), minZ = gatherOrigin.getZ();
        int maxX = gatherOrigin.getX(), maxY = gatherOrigin.getY(), maxZ = gatherOrigin.getZ();
        int blockCount = 0;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        while (!frontier.isEmpty()) {
            final Pair<BlockPos, BlockState> pair = frontier.removeFirst();
            final BlockPos pos = pair.key();
            blockCount++;
            if (blockCount > maximumBlocksToAssemble)
                return new SubLevelAssemblyHelper.GatherResult(null, blockCount, null, SubLevelAssemblyHelper.GatherResult.State.TOO_MANY_BLOCKS);
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            blocks.add(pos);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0)
                            continue;
                        final int absTotal = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (absTotal == 3)
                            continue;
                        final BlockPos candidate = mutablePos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                        if (frontier.contains(candidate))
                            continue;
                        final BlockState candidateState = accelerator.getBlockState(candidate);
                        if (candidateState.isAir() || blacklist.stream().anyMatch(candidateState::is) || candidateState.getBlock() instanceof LiquidBlock)
                            continue;
                        if (!blocks.contains(candidate)) {
                            frontier.add(Pair.of(candidate.immutable(), candidateState));
                        }
                    }
                }
            }
        }
        final BoundingBox3i bounds = new BoundingBox3i(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
        if (blocks.isEmpty())
            return new SubLevelAssemblyHelper.GatherResult(null, blockCount, null, SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS);

        return new SubLevelAssemblyHelper.GatherResult(blocks, blockCount, bounds, SubLevelAssemblyHelper.GatherResult.State.SUCCESS);
    }

}
