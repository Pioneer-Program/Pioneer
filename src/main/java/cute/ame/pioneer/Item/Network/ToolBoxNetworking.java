package cute.ame.pioneer.Item.Network;

import cute.ame.pioneer.Item.Client.Name.NameClient;
import cute.ame.pioneer.Item.Client.Readout.ToolBoxReadoutHud;
import cute.ame.pioneer.Item.Client.Tune.TuneClient;
import cute.ame.pioneer.Item.Name.NameServer;
import cute.ame.pioneer.Item.Name.ToolBoxColors;
import cute.ame.pioneer.Item.Readout.ToolBoxReadouts;
import cute.ame.pioneer.Item.ToolBoxItem;
import cute.ame.pioneer.Item.Tune.TuneServer;
import cute.ame.pioneer.Pioneer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Pioneer.MODID)
public final class ToolBoxNetworking
{
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Pioneer.MODID).versioned("1");

        registrar.playToServer(ToolBoxModePayload.TYPE, ToolBoxModePayload.CODEC, (payload, context) -> context.enqueueWork(() -> ToolBoxItem.cycleMode(context.player(), payload.forward())));
        registrar.playToServer(ToolBoxWatchPayload.TYPE, ToolBoxWatchPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
                ToolBoxReadouts.watch(player, payload.watching(), payload.pos());
        }));
        registrar.playToClient(ToolBoxReadoutPayload.TYPE, ToolBoxReadoutPayload.CODEC, (payload, context) -> context.enqueueWork(() -> ToolBoxReadoutHud.receive(payload)));
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
        registrar.playToClient(ToolBoxColorsPayload.TYPE, ToolBoxColorsPayload.CODEC, (payload, context) -> context.enqueueWork(() -> ToolBoxColors.replace(payload.entries())));
    }
}
