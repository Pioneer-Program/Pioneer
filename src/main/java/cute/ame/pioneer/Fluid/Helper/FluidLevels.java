package cute.ame.pioneer.Fluid.Helper;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class FluidLevels
{
    public record Located(ServerLevel level, BlockPos pos, @Nullable SubLevel subLevel)
    {
        public boolean onSubLevel() { return subLevel != null; }
    }

    public static Located resolve(ServerLevel level, Vec3 worldPos)
    {
        BlockPos direct = BlockPos.containing(worldPos);

        SubLevel owner = Sable.HELPER.getContaining(level, direct);
        if (owner != null) return new Located(level, direct, owner);

        AABB probe = new AABB(worldPos, worldPos).inflate(0.5);
        for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(probe)))
        {
            Vec3 local = sub.logicalPose().transformPositionInverse(worldPos);
            if (!sub.getPlot().contains(local)) continue;

            return new Located(level, BlockPos.containing(local), sub);
        }

        return new Located(level, direct, null);
    }

    public static boolean isLoaded(ServerLevel level, BlockPos pos)
    {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null && container.inBounds(pos)) return container.getChunk(new ChunkPos(pos)) != null;

        return level.hasChunkAt(pos);
    }
}
