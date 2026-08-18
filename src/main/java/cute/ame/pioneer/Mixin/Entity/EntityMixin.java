package cute.ame.pioneer.Mixin.Entity;

import cute.ame.pioneer.Core.API.PioneerAPI;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private Level level;

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract void setPos(double x, double y, double z);

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void moveHead(MoverType type, Vec3 pos, CallbackInfo ci) {
        if (!(((Entity) (Object) this) instanceof ServerPlayer player))
            return;
        double newX = this.getX() + pos.x;
        double newZ = this.getZ() + pos.z;
        double speed = Math.sqrt(pos.x * pos.x + pos.z * pos.z);
        Pioneer.SPEED_MAP.put(player, speed);
        if (PioneerAPI.isSpaceDimension(level.dimension()) && speed > 300.D) {
            this.setPos(newX, this.getY() + pos.y, newZ);
            ci.cancel();
        }
    }

}
