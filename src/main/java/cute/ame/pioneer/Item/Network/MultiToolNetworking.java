package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Item.Client.Readout.MultiToolReadoutHud;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.Readout.MultiToolReadouts;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class MultiToolNetworking
{
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Pioneer.MODID).versioned("1");

        registrar.playToServer(MultiToolModePayload.TYPE, MultiToolModePayload.CODEC, (payload, context) -> context.enqueueWork(() -> MultiToolItem.cycleMode(context.player(), payload.forward())));
        registrar.playToServer(MultiToolWatchPayload.TYPE, MultiToolWatchPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
                MultiToolReadouts.watch(player, payload.watching(), payload.pos());
        }));
        registrar.playToClient(MultiToolReadoutPayload.TYPE, MultiToolReadoutPayload.CODEC, (payload, context) -> context.enqueueWork(() -> MultiToolReadoutHud.receive(payload)));
    }
}
