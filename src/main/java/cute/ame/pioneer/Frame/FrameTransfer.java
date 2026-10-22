package cute.ame.pioneer.Frame;

import cute.ame.pioneer.Core.API.Frame.FrameReport;
import cute.ame.pioneer.Core.Frame.FrameShift;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
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
    private static final byte FREE = 0, RIDER = 1, SEATED = 2, MOUNTED = 3;
    private static final double TICKS_PER_SECOND = 20.0;

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

    public static FrameReport carry(ServerLevel level, List<ServerSubLevel> subLevels, List<ServerPlayer> players, List<Entity> entities, FrameShift shift)
    {
        FrameReport report = new FrameReport();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return report;

        PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
        int playerCount = players.size();
        byte[] kind = new byte[playerCount];
        Entity[] mounts = null;
        int mountCount = 0;
        for (int i = 0; i < playerCount; ++i)
        {
            ServerPlayer player = players.get(i);
            SubLevel seat = Sable.HELPER.getVehicleSubLevel(player);
            if (contains(subLevels, seat))
            {
                kind[i] = SEATED;
                continue;
            }

            if (player.isPassenger())
            {
                Entity mount = player.getRootVehicle();
                if (seat == null && !(mount instanceof Player) && !mount.isRemoved() && Sable.HELPER.getContaining(mount) == null)
                {
                    kind[i] = MOUNTED;
                    if (mounts == null) mounts = new Entity[playerCount];
                    if (!contains(mounts, mountCount, mount)) mounts[mountCount++] = mount;

                    continue;
                }

                player.stopRiding();
            }

            kind[i] = contains(subLevels, Sable.HELPER.getTrackingSubLevel(player)) ? RIDER : FREE;
        }

        report.tick = container.trackingSystem().getInterpolationTick();
        double[] p = new double[4];
        Vector3d target = new Vector3d();
        Quaterniond turn = new Quaterniond();
        Vector3d linear = new Vector3d(), angular = new Vector3d();
        boolean turns = shift.rotated || shift.spins();
        boolean kick = shift.kicks();
        List<UUID> moved = new ArrayList<>(subLevels.size());
        for (ServerSubLevel subLevel : subLevels)
        {
            if (subLevel.isRemoved()) continue;

            Vector3d at = subLevel.logicalPose().position();
            shift.position(at.x, at.y, at.z, p);
            target.set(p[0], p[1], p[2]);
            Quaterniondc o = subLevel.logicalPose().orientation();
            shift.orientation(o.x(), o.y(), o.z(), o.w(), p);
            turn.set(p[0], p[1], p[2], p[3]);
            pipeline.teleport(subLevel, target, turn);

            subLevel.updateLastPose();
            subLevel.updateBoundingBox();
            if (turns) turnVelocity(pipeline, subLevel, shift, target, linear, angular, p);
            else if (kick)
                pipeline.addLinearAndAngularVelocity(subLevel, linear.set(shift.lx, shift.ly, shift.lz), ZERO);

            moved.add(subLevel.getUniqueId());
            report.subLevels++;
        }

        int[] shifted = new int[playerCount + entities.size() + mountCount];
        int n = 0;
        for (int i = 0; i < playerCount; ++i) if (kind[i] == RIDER) shifted[n++] = players.get(i).getId();

        int riders = n;
        for (int i = 0; i < playerCount; ++i)
            if (kind[i] == FREE || kind[i] == MOUNTED) shifted[n++] = players.get(i).getId();

        for (Entity entity : entities)
        {
            if (entity.isRemoved() || entity.isPassenger() || contains(mounts, mountCount, entity)) continue;

            shifted[n++] = entity.getId();
        }

        for (int i = 0; i < mountCount; ++i) shifted[n++] = mounts[i].getId();

        if (n != shifted.length) shifted = Arrays.copyOf(shifted, n);

        PacketDistributor.sendToPlayersInDimension(level, FrameShiftPayload.of(report.tick, shift, moved, shifted, riders));
        for (int i = 0; i < playerCount; ++i)
        {
            ServerPlayer player = players.get(i);
            if (kind[i] == SEATED || kind[i] == MOUNTED)
            {
                report.seated++;
                continue;
            }

            shift.position(player.getX(), player.getY(), player.getZ(), p);
            double x = p[0], y = p[1], z = p[2];
            if (shift.rotated && kind[i] != RIDER) turn(player, shift, x, y, z, p);
            else player.absMoveTo(x, y, z);

            if (kind[i] == RIDER)
            {
                report.riders++;
                continue;
            }

            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot(), ALL_RELATIVE);
            if (kick || shift.rotated) push(player, shift, x, y, z, p);

            report.free++;
        }

        for (Entity entity : entities)
        {
            if (entity.isRemoved() || entity.isPassenger() || contains(mounts, mountCount, entity)) continue;

            move(entity, shift, kick, p);
            report.entities++;
        }

        for (int i = 0; i < mountCount; ++i)
        {
            move(mounts[i], shift, kick, p);
            report.entities++;
        }

        return report;
    }

    private static void turnVelocity(PhysicsPipeline pipeline, ServerSubLevel subLevel, FrameShift shift, Vector3d at, Vector3d linear, Vector3d angular, double[] p)
    {
        pipeline.getLinearVelocity(subLevel, linear);
        pipeline.getAngularVelocity(subLevel, angular);
        shift.direction(linear.x, linear.y, linear.z, p);
        double vx = p[0], vy = p[1], vz = p[2];
        shift.kick(at.x, at.y, at.z, p);
        linear.set(vx + p[0] - linear.x, vy + p[1] - linear.y, vz + p[2] - linear.z);
        shift.direction(angular.x, angular.y, angular.z, p);
        angular.set(p[0] + shift.wx - angular.x, p[1] + shift.wy - angular.y, p[2] + shift.wz - angular.z);
        pipeline.addLinearAndAngularVelocity(subLevel, linear, angular);
    }

    private static void turn(ServerPlayer player, FrameShift shift, double x, double y, double z, double[] p)
    {
        float yaw = player.getYRot();
        shift.look(yaw, player.getXRot(), p);
        turnHead(player, (float) p[0] - yaw);
        player.absMoveTo(x, y, z, (float) p[0], (float) p[1]);
    }

    private static void move(Entity entity, FrameShift shift, boolean kick, double[] p)
    {
        shift.position(entity.getX(), entity.getY(), entity.getZ(), p);
        double x = p[0], y = p[1], z = p[2];
        if (shift.rotated)
        {
            float yaw = entity.getYRot();
            shift.look(yaw, entity.getXRot(), p);
            turnHead(entity, (float) p[0] - yaw);
            entity.setYRot((float) p[0]);
            entity.setXRot((float) p[1]);
        }

        entity.teleportTo(x, y, z);
        if (kick || shift.rotated) push(entity, shift, x, y, z, p);
    }

    private static void push(Entity entity, FrameShift shift, double x, double y, double z, double[] p)
    {
        Vec3 d = entity.getDeltaMovement();
        shift.direction(d.x, d.y, d.z, p);
        double vx = p[0], vy = p[1], vz = p[2];
        shift.kick(x, y, z, p);
        entity.setDeltaMovement(vx + p[0] / TICKS_PER_SECOND, vy + p[1] / TICKS_PER_SECOND, vz + p[2] / TICKS_PER_SECOND);
        entity.hurtMarked = true;
    }

    private static void turnHead(Entity entity, float delta)
    {
        entity.setYHeadRot(entity.getYHeadRot() + delta);
        if (entity instanceof LivingEntity living) living.yBodyRot += delta;
    }

    private static boolean contains(List<ServerSubLevel> subLevels, @Nullable SubLevel subLevel)
    {
        if (subLevel == null) return false;
        for (int i = 0, n = subLevels.size(); i < n; i++) if (subLevels.get(i) == subLevel) return true;

        return false;
    }

    private static boolean contains(@Nullable Entity[] entities, int count, Entity entity)
    {
        for (int i = 0; i < count; i++) if (entities[i] == entity) return true;

        return false;
    }
}
