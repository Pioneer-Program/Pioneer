package cute.ame.pioneer.Event;

import cute.ame.pioneer.Core.Observer.CubeSurface;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.Core.Observer.PlanetCube;
import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;
import java.util.Set;

// Purely security, should not be trigger on bigger version
@EventBusSubscriber(modid = Pioneer.MODID)
public final class FaceCrossingHandler
{
    private static final int SAFETY_SCAN_HEIGHT = 8;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isPassenger()) return;

        Optional<PlanetDefinition> hostOpt = ObserverStates.surfaceHost(player.level());
        if (hostOpt.isEmpty()) return;
        PlanetDefinition body = hostOpt.get();

        double halfSide = PlanetCube.halfSide(body);
        int face = PlanetCube.faceOf(body, player.getX());
        double u = PlanetCube.toU(body, face, player.getX());
        double v = PlanetCube.toV(body, player.getZ());

        if (Math.abs(u) <= 1.0 && Math.abs(v) <= 1.0) return;

        CubeSurface.Crossing crossing = CubeSurface.cross(halfSide, face, u, v);
        if (crossing == null)
        {
            pushBackInside(player, body, face, u, v, halfSide);
            return;
        }

        cross(player, face, crossing);
    }

    private static void cross(ServerPlayer player, int fromFace, CubeSurface.Crossing crossing)
    {
        ServerLevel level = (ServerLevel) player.level();

        double[] heading = new double[2];
        double yawRad = Math.toRadians(player.getYRot());
        CubeSurface.transportHeading(fromFace, crossing.face(), -Math.sin(yawRad), Math.cos(yawRad), heading);
        float newYaw = CubeSurface.yawOf(heading[0], heading[1]);

        Vec3 motion = player.getDeltaMovement();
        double[] transported = new double[2];
        CubeSurface.transportHeading(fromFace, crossing.face(), motion.x, motion.z, transported);

        double x = crossing.blockX();
        double z = crossing.blockZ();
        double y = safeY(level, x, player.getY(), z);

        player.teleportTo(level, x, y, z, Set.of(), newYaw, player.getXRot());
        player.setDeltaMovement(transported[0], motion.y, transported[1]);
        player.hurtMarked = true;

        Pioneer.LOGGER.debug("[Pioneer] {} crossed face {} -> {} at ({}, {})", player.getScoreboardName(), fromFace, crossing.face(), (long) x, (long) z);
    }

    private static double safeY(ServerLevel level, double x, double y, double z)
    {
        BlockPos pos = BlockPos.containing(x, y, z);
        level.getChunk(pos);

        for (int i = 0; i < SAFETY_SCAN_HEIGHT; i++)
        {
            BlockPos probe = pos.above(i);
            if (level.getBlockState(probe).isAir() && level.getBlockState(probe.above()).isAir())
                return y + i;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) + 1;
    }

    private static void pushBackInside(ServerPlayer player, PlanetDefinition body, int face, double u, double v, double halfSide)
    {
        double x = PlanetCube.faceOriginX(body, face) + Math.clamp(u, -1.0, 1.0) * halfSide;
        double z = Math.clamp(v, -1.0, 1.0) * halfSide;

        player.teleportTo((ServerLevel) player.level(), x, player.getY(), z, Set.of(), player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }
}
