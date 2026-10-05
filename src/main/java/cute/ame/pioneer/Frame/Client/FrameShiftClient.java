package cute.ame.pioneer.Frame.Client;

import cute.ame.pioneer.Frame.Network.FrameShiftPayload;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public final class FrameShiftClient
{
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

        double dx = p.dx(), dy = p.dy(), dz = p.dz();
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null)
        {
            for (UUID id : p.subLevels())
            {
                SubLevel subLevel = container.getSubLevel(id);
                if (subLevel instanceof ShiftableSubLevel shiftable) shiftable.pioneer$shift(p.tick(), dx, dy, dz);
            }
        }

        for (int id : p.entities())
        {
            Entity entity = level.getEntity(id);
            if (entity != null) shift(entity, dx, dy, dz);
        }
    }

    private static void shift(Entity entity, double dx, double dy, double dz)
    {
        double x = entity.getX() + dx, y = entity.getY() + dy, z = entity.getZ() + dz;
        entity.setPos(x, y, z);
        entity.xo += dx;
        entity.yo += dy;
        entity.zo += dz;
        entity.xOld += dx;
        entity.yOld += dy;
        entity.zOld += dz;
        if (!(entity instanceof LocalPlayer)) entity.lerpTo(x, y, z, entity.getYRot(), entity.getXRot(), 0);
    }
}
