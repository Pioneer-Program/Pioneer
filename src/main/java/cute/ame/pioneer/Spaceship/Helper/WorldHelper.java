package cute.ame.pioneer.Spaceship.Helper;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Spaceship.Data.StoredBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public class WorldHelper {

    private final static int RADIUS = 256;

    public static double getVectorLength(int x, int y, int z) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public static double getVectorLength(Vec3i vec) {
        return Math.sqrt(Math.pow(vec.getX(), 2) + Math.pow(vec.getY(), 2)  + Math.pow(vec.getZ(), 2));
    }

    public static Vec3i nearestFreeSpaceOffset(Level level, Set<StoredBlock> blocks) {
        Vec3i nearest = Vec3i.ZERO;
        int maxHeight = level.getMaxBuildHeight();
        int halfRadius = RADIUS / 2;
        double nearestLength = Double.MAX_VALUE;
        for (int x = 0; x < RADIUS; ++x) {
            int realX = x;
            if (x > halfRadius)
                realX = halfRadius - x;
            if (Math.abs(realX) >= nearestLength)
                break;
            for (int y = 0; y < RADIUS; ++y) {
                int realY = y;
                if (y > halfRadius)
                    realY = halfRadius - y;
                if (getVectorLength(realX, realY, 0) >= nearestLength)
                    break;
                for (int z = 0; z < RADIUS; ++z) {
                    int realZ = z;
                    if (z > halfRadius)
                        realZ = halfRadius - z;
                    if (getVectorLength(realX, realY, realZ) >= nearestLength)
                        break;
                    boolean placeable = true;
                    for (StoredBlock block : blocks) {
                        BlockPos pos =  block.pos().offset(realX, realY, realZ);
                        if (pos.getY() >= maxHeight) {
                            placeable = false;
                            break;
                        }
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
                            placeable = false;
                            break;
                        }
                    }
                    if (placeable) {
                        Vec3i offset = new Vec3i(realX, realY, realZ);
                        double length = getVectorLength(offset);
                        if (length < nearestLength) {
                            nearest = offset;
                            nearestLength = length;
                        }
                    }
                }
            }
        }
        if (nearestLength == Double.MAX_VALUE)
            Pioneer.LOGGER.error("nearest free space offset error");
        return nearest;
    }

}
