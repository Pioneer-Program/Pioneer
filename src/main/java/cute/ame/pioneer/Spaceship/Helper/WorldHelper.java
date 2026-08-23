package cute.ame.pioneer.Spaceship.Helper;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Spaceship.Data.StoredBlock;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
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
        double nearestLength = Double.MAX_VALUE;
        for (int x = 0; x < RADIUS; ++x) {
            if (x >= nearestLength)
                break;
            for (int y = 0; y < RADIUS; ++y) {
                if (getVectorLength(x, y, 0) >= nearestLength)
                    break;
                for (int z = 0; z < RADIUS; ++z) {
                    if (getVectorLength(x, y, z) >= nearestLength)
                        break;
                    boolean placeable = true;
                    for (StoredBlock block : blocks) {
                        if (!level.getBlockState(block.pos().offset(x, y, z)).isAir()) {
                            placeable = false;
                            break;
                        }
                    }
                    if (placeable) {
                        Vec3i offset = new Vec3i(x, y, z);
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
