package cute.ame.pioneer.Mixin.Frame;

import cute.ame.pioneer.Frame.HeldMoves;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HeldMovesMixin implements HeldMoves
{
    @Shadow
    public ServerPlayer player;

    @Unique
    private int pioneer$shifts;
    @Unique
    private long pioneer$since;

    @Shadow
    public abstract void resetPosition();

    @Override
    public void pioneer$hold()
    {
        ++pioneer$shifts;
        pioneer$since = player.serverLevel().getGameTime();
        resetPosition();
    }

    @Override
    public void pioneer$confirm()
    {
        if (pioneer$shifts > 0) --pioneer$shifts;
    }

    @Override
    public int pioneer$heldShifts()
    {
        return pioneer$shifts;
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void pioneer$dropHeldMove(ServerboundMovePlayerPacket packet, CallbackInfo ci)
    {
        if (pioneer$shifts == 0) return;
        if (player.serverLevel().getGameTime() - pioneer$since <= 40L)
        {
            ci.cancel();
            return;
        }

        pioneer$shifts = 0;
    }
}
