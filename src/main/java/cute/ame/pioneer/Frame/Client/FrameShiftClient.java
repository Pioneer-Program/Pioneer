package cute.ame.pioneer.Frame.Client;

import cute.ame.pioneer.Core.Frame.FrameLook;
import cute.ame.pioneer.Core.Frame.FrameShift;
import cute.ame.pioneer.Frame.Network.FrameShiftAckPayload;
import cute.ame.pioneer.Frame.Network.FrameShiftPayload;
import cute.ame.pioneer.Pioneer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;

import java.util.UUID;

@EventBusSubscriber(modid = Pioneer.MODID, value = Dist.CLIENT)
public final class FrameShiftClient
{
    private static final FrameShift SHIFT = new FrameShift();
    private static final double SABLE_TURN_LIMIT = 1.0e-6;
    private static final Vector3d P = new Vector3d();
    private static boolean bobPending;
    private static float bobLag, bobLagO;

    public static void apply(FrameShiftPayload p)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread())
        {
            minecraft.execute(() -> apply(p));
            return;
        }

        ClientLevel level = minecraft.level;
        if (level == null) return;

        FrameShift shift = p.shift(SHIFT);
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null)
        {
            for (UUID id : p.subLevels())
            {
                SubLevel subLevel = container.getSubLevel(id);
                if (subLevel instanceof ShiftableSubLevel shiftable) shiftable.pioneer$shift(p.tick(), shift);
            }
        }

        int[] entities = p.entities();
        for (int i = 0; i < entities.length; ++i)
        {
            Entity entity = level.getEntity(entities[i]);
            if (entity != null) shift(entity, shift, i >= p.riders());
        }

        confirmRider(minecraft.player, entities, p.riders());
    }

    private static void confirmRider(LocalPlayer player, int[] entities, int riders)
    {
        if (player == null) return;

        int id = player.getId();
        for (int i = 0; i < riders; ++i)
        {
            if (entities[i] != id) continue;

            PacketDistributor.sendToServer(FrameShiftAckPayload.INSTANCE);
            return;
        }
    }

    private static void shift(Entity entity, FrameShift shift, boolean look)
    {
        shift.position(entity.xo, entity.yo, entity.zo, P);
        entity.xo = P.x;
        entity.yo = P.y;
        entity.zo = P.z;
        shift.position(entity.xOld, entity.yOld, entity.zOld, P);
        entity.xOld = P.x;
        entity.yOld = P.y;
        entity.zOld = P.z;
        shift.position(entity.getX(), entity.getY(), entity.getZ(), P);
        double x = P.x, y = P.y, z = P.z;
        entity.setPos(x, y, z);
        if (shift.rotated) turn(entity, shift, look);

        if (!(entity instanceof LocalPlayer)) entity.lerpTo(x, y, z, entity.getYRot(), entity.getXRot(), 0);
    }

    private static void turn(Entity entity, FrameShift shift, boolean look)
    {
        Vec3 d = entity.getDeltaMovement();
        shift.direction(d.x, d.y, d.z, P);
        entity.setDeltaMovement(P.x, P.y, P.z);
        if (!look)
        {
            if (entity instanceof LocalPlayer player) completeSableTurn(player, shift);

            return;
        }

        float yaw = entity.getYRot();
        shift.look(yaw, entity.getXRot(), P);
        float turnedYaw = FrameLook.yaw(P, yaw);
        float delta = turnedYaw - yaw;
        entity.setYRot(turnedYaw);
        entity.setXRot(FrameLook.pitch(P));
        entity.yRotO += delta;
        entity.xRotO = entity.getXRot();
        if (entity instanceof LivingEntity living)
        {
            living.yHeadRot += delta;
            living.yHeadRotO += delta;
            living.yBodyRot += delta;
            living.yBodyRotO += delta;
        }

        if (entity instanceof LocalPlayer player)
        {
            player.yBob += delta;
            player.yBobO += delta;
            player.xBob = player.getXRot();
            player.xBobO = player.getXRot();
        }
    }

    private static void completeSableTurn(LocalPlayer player, FrameShift shift)
    {
        if (Math.abs(shift.qw) < SABLE_TURN_LIMIT) return;

        float yaw = player.getYRot(), pitch = player.getXRot();
        bobLag = yaw - player.yBob;
        bobLagO = player.yRotO - player.yBobO;
        bobPending = true;
        shift.look(yaw, pitch, P);
        double sableYaw = -Math.toDegrees(2.0 * shift.qy / shift.qw);
        float dYaw = (float) Mth.wrapDegrees(Mth.wrapDegrees((double) FrameLook.yaw(P, yaw) - yaw) - sableYaw);
        float dPitch = FrameLook.pitch(P) - pitch;
        player.setYRot(yaw + dYaw);
        player.setXRot(pitch + dPitch);
        player.yRotO += dYaw;
        player.xRotO += dPitch;
        player.yHeadRot += dYaw;
        player.yHeadRotO += dYaw;
        player.yBodyRot += dYaw;
        player.yBodyRotO += dYaw;
        player.xBob += dPitch;
        player.xBobO += dPitch;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (!bobPending) return;

        bobPending = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        player.yBob = player.getYRot() - bobLag;
        player.yBobO = player.yRotO - bobLagO;
    }
}
