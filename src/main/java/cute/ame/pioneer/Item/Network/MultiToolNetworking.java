package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Item.Client.Name.NameClient;
import cute.ame.pioneer.Item.Client.Readout.MultiToolReadoutHud;
import cute.ame.pioneer.Item.Client.Tune.TuneClient;
import cute.ame.pioneer.Item.MultiToolItem;
import cute.ame.pioneer.Item.Name.MultiToolColors;
import cute.ame.pioneer.Item.Name.NameServer;
import cute.ame.pioneer.Item.Readout.MultiToolReadouts;
import cute.ame.pioneer.Item.Tune.TuneServer;
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
        registrar.playToServer(TuneRequestPayload.TYPE, TuneRequestPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player) TuneServer.request(player, payload.pos());
        }));
        registrar.playToServer(TuneSetPayload.TYPE, TuneSetPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
                TuneServer.set(player, payload.pos(), payload.option(), payload.value());
        }));
        registrar.playToClient(TuneOptionsPayload.TYPE, TuneOptionsPayload.CODEC, (payload, context) -> context.enqueueWork(() -> TuneClient.receive(payload)));
        registrar.playToServer(NameRequestPayload.TYPE, NameRequestPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player) NameServer.request(player);
        }));
        registrar.playToServer(NameSetPayload.TYPE, NameSetPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player) NameServer.set(player, payload.pos(), payload.label());
        }));
        registrar.playToClient(NameLabelsPayload.TYPE, NameLabelsPayload.CODEC, (payload, context) -> context.enqueueWork(() -> NameClient.receive(payload)));
        registrar.playToClient(MultiToolColorsPayload.TYPE, MultiToolColorsPayload.CODEC, (payload, context) -> context.enqueueWork(() -> MultiToolColors.replace(payload.entries())));
    }
}
