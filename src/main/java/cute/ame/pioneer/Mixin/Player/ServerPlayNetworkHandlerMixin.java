package cute.ame.pioneer.Mixin.Player;


import cute.ame.pioneer.Core.API.PioneerAPI;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void trackSpeed(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        double newX = packet.getX(player.getX());
        double newZ = packet.getZ(player.getZ());
        double dx = newX - PioneerAPI.ChunkLoadState.getLastX(player);
        double dz = newZ - PioneerAPI.ChunkLoadState.getLastZ(player);
        double dist = Math.sqrt(dx * dx + dz * dz);
        PioneerAPI.ChunkLoadState.setSpeed(player, dist);
        PioneerAPI.ChunkLoadState.setLastPos(player, newX, newZ);
    }
}
