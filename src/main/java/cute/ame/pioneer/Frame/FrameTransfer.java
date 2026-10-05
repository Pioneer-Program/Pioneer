package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Core.API.Frame.FrameReport;
import cute.ame.pioneer.Frame.Network.FrameShiftPayload;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class FrameTransfer
{
    private static final Set<RelativeMovement> ALL_RELATIVE = EnumSet.allOf(RelativeMovement.class);
    private static final Vector3d ZERO = new Vector3d();
    private static final byte FREE = 0, RIDER = 1, SEATED = 2;

    public static double massOf(ServerSubLevel subLevel)
    {
        MassData mass = subLevel.getMassTracker();
        return mass == null ? 0.0 : mass.getMass();
    }

    public static String shortId(SubLevel subLevel)
    {
        return subLevel.getUniqueId().toString().substring(0, 8);
    }

    public static Vector3d velocityOf(ServerLevel level, ServerSubLevel subLevel, Vector3d dest)
    {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || subLevel.isRemoved()) return dest.zero();

        return container.physicsSystem().getPipeline().getLinearVelocity(subLevel, dest);
    }

    public static FrameReport carry(ServerLevel level, List<ServerSubLevel> subLevels, List<ServerPlayer> players, List<Entity> entities, double dx, double dy, double dz, double dvx, double dvy, double dvz)
    {
        FrameReport report = new FrameReport();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return report;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        int playerCount = players.size();
        byte[] kind = new byte[playerCount];
        for (int i = 0; i < playerCount; i++)
        {
            ServerPlayer player = players.get(i);
            if (indexOf(subLevels, Sable.HELPER.getVehicleSubLevel(player)) >= 0)
            {
                kind[i] = SEATED;
                continue;
            }

            if (player.isPassenger()) player.stopRiding();
            kind[i] = indexOf(subLevels, Sable.HELPER.getTrackingSubLevel(player)) >= 0 ? RIDER : FREE;
        }

        report.tick = container.trackingSystem().getInterpolationTick();
        Vector3d target = new Vector3d();
        Vector3d dv = new Vector3d(dvx, dvy, dvz);
        boolean kick = dv.lengthSquared() > 0.0;
        List<UUID> moved = new ArrayList<>(subLevels.size());
        for (ServerSubLevel subLevel : subLevels)
        {
            if (subLevel.isRemoved()) continue;
            target.set(subLevel.logicalPose().position()).add(dx, dy, dz);
            pipeline.teleport(subLevel, target, subLevel.logicalPose().orientation());

            subLevel.updateLastPose();
            subLevel.updateBoundingBox();
            if (kick) pipeline.addLinearAndAngularVelocity(subLevel, dv, ZERO);

            moved.add(subLevel.getUniqueId());
            report.subLevels++;
        }

        int[] shifted = new int[playerCount + entities.size()];
        int n = 0;
        for (int i = 0; i < playerCount; i++) if (kind[i] != SEATED) shifted[n++] = players.get(i).getId();
        for (Entity entity : entities) if (!entity.isRemoved() && !entity.isPassenger()) shifted[n++] = entity.getId();
        if (n != shifted.length) shifted = Arrays.copyOf(shifted, n);

        PacketDistributor.sendToPlayersInDimension(level, new FrameShiftPayload(report.tick, dx, dy, dz, moved, shifted));
        double tvx = dvx / 20.0, tvy = dvy / 20.0, tvz = dvz / 20.0;
        for (int i = 0; i < playerCount; i++)
        {
            ServerPlayer player = players.get(i);
            if (kind[i] == SEATED)
            {
                report.seated++;
                continue;
            }

            double x = player.getX() + dx, y = player.getY() + dy, z = player.getZ() + dz;
            player.absMoveTo(x, y, z);
            if (kind[i] == RIDER)
            {
                report.riders++;
                continue;
            }

            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot(), ALL_RELATIVE);
            if (kick)
            {
                player.setDeltaMovement(player.getDeltaMovement().add(tvx, tvy, tvz));
                player.hurtMarked = true;
            }

            report.free++;
        }

        for (Entity entity : entities)
        {
            if (entity.isRemoved() || entity.isPassenger()) continue;

            entity.teleportTo(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz);
            if (kick)
            {
                entity.setDeltaMovement(entity.getDeltaMovement().add(tvx, tvy, tvz));
                entity.hurtMarked = true;
            }

            report.entities++;
        }

        return report;
    }

    private static int indexOf(List<ServerSubLevel> subLevels, @Nullable SubLevel subLevel)
    {
        if (subLevel == null) return -1;
        for (int i = 0, n = subLevels.size(); i < n; i++) if (subLevels.get(i) == subLevel) return i;

        return -1;
    }
}
