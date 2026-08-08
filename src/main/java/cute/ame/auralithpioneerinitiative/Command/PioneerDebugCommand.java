package cute.ame.auralithpioneerinitiative.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cute.ame.auralithpioneerinitiative.Seamless.Network.PreloadCancelPayload;
import cute.ame.auralithpioneerinitiative.Seamless.Network.PreloadDimensionPayload;
import cute.ame.auralithpioneerinitiative.Seamless.SeamlessChunkStreamer;
import cute.ame.auralithpioneerinitiative.Seamless.SeamlessPreloadManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PioneerDebugCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("pioneerdeb")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("seamless-test-preload").then(Commands.argument("dimension", DimensionArgument.dimension()).then(Commands.argument("radiusChunks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 32)).executes(PioneerDebugCommand::executePreload))))
            .then(Commands.literal("seamless-test-preload-planet").then(Commands.argument("surfaceDimension", DimensionArgument.dimension()).then(Commands.argument("radiusChunks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 32)).executes(PioneerDebugCommand::executePreloadPlanet))))
            .then(Commands.literal("seamless-test-cancel").then(Commands.argument("dimension", DimensionArgument.dimension()).executes(PioneerDebugCommand::executeCancel)))
            .then(Commands.literal("seamless-test-stream").then(Commands.argument("dimension", DimensionArgument.dimension()).executes(PioneerDebugCommand::executeStream)))
            .then(Commands.literal("seamless-test-render").then(Commands.argument("dimension", DimensionArgument.dimension()).executes(PioneerDebugCommand::executeRender)))
        );
    }

    private static int executeRender(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerLevel targetLevel = DimensionArgument.getDimension(ctx, "dimension");
        ResourceKey<Level> targetDim = targetLevel.dimension();

        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
        {
            source.sendFailure(Component.literal("[Auralith][debug] seamless-test-render requires a physical client (singleplayer/LAN host) — not available on a dedicated server"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("[Auralith][debug] Ghost debug frame capture for " + targetDim.location() + " queued on the render thread — single frame, check the client log for the result"), false);

        return 1;
    }

    private static int executeStream(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel targetLevel = DimensionArgument.getDimension(ctx, "dimension");
        ResourceKey<Level> targetDim = targetLevel.dimension();

        var forced = SeamlessPreloadManager.getForcedSnapshot(player.getUUID(), targetDim);
        if (forced.isEmpty())
        {
            source.sendFailure(Component.literal("[Auralith][debug] Nothing forced for " + targetDim.location() + " — run seamless-test-preload first"));
            return 0;
        }

        int sentThisCall = SeamlessChunkStreamer.streamReadyChunks(player, targetLevel, forced);
        source.sendSuccess(() -> Component.literal("[Auralith][debug] Streamed " + sentThisCall + " new chunk(s) this call (" + forced.size() + " total forced) for " + targetDim.location()), false);
        return 1;
    }

    private static int executePreload(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel targetLevel = DimensionArgument.getDimension(ctx, "dimension");
        int radiusChunks = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radiusChunks");

        ResourceKey<Level> fromDim = player.level().dimension();
        ResourceKey<Level> targetDim = targetLevel.dimension();

        BlockPos anchor = player.blockPosition();
        SeamlessPreloadManager.preload(player.getUUID(), targetLevel, anchor, radiusChunks);
        PacketDistributor.sendToPlayer(player, new PreloadDimensionPayload(fromDim, targetDim, anchor, targetLevel.dimensionTypeRegistration().unwrapKey().orElseThrow()));
        source.sendSuccess(() -> Component.literal("[Auralith][debug] Preloading " + radiusChunks + " chunk radius around " + anchor.toShortString() + " in " + targetDim.location() + " (server ticket + client hint sent)"), false);
        return 1;
    }

    private static int executePreloadPlanet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel targetLevel = DimensionArgument.getDimension(ctx, "surfaceDimension");
        ResourceKey<Level> targetDim = targetLevel.dimension();
        int radiusChunks = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radiusChunks");

        ResourceKey<Level> fromDim = player.level().dimension();

        var bindingOpt = cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI.getBindingForDimension(targetDim);
        if (bindingOpt.isEmpty())
        {
            source.sendFailure(Component.literal("[Auralith][debug] " + targetDim.location() + " has no known dimension binding (not registered to a solar system) — can't resolve a planet"));
            return 0;
        }

        var binding = bindingOpt.get();
        var systemOpt = cute.ame.auralithpioneerinitiative.Core.API.AuralithAPI.getSolarSystem(binding.systemId());
        if (systemOpt.isEmpty())
        {
            source.sendFailure(Component.literal("[Auralith][debug] Solar system " + binding.systemId() + " not found for " + targetDim.location()));
            return 0;
        }

        var planetOpt = systemOpt.get().findById(binding.planetId());
        if (planetOpt.isEmpty())
        {
            source.sendFailure(Component.literal("[Auralith][debug] Planet " + binding.planetId() + " not found in system " + binding.systemId()));
            return 0;
        }

        BlockPos anchor = cute.ame.auralithpioneerinitiative.Seamless.TransitionManager.surfaceLandingColumn(player, planetOpt.get());

        SeamlessPreloadManager.preload(player.getUUID(), targetLevel, anchor, radiusChunks);
        PacketDistributor.sendToPlayer(player, new PreloadDimensionPayload(fromDim, targetDim, anchor, targetLevel.dimensionTypeRegistration().unwrapKey().orElseThrow()));

        source.sendSuccess(() -> Component.literal("[Auralith][debug] Preloading " + radiusChunks + " chunk radius around planet-projected anchor " + anchor.toShortString() + " in " + targetDim.location() + " (server ticket + client hint sent)"), false);
        return 1;
    }

    private static int executeCancel(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel targetLevel = DimensionArgument.getDimension(ctx, "dimension");
        ResourceKey<Level> targetDim = targetLevel.dimension();

        SeamlessPreloadManager.release(player.getUUID(), targetDim, targetLevel);
        PacketDistributor.sendToPlayer(player, new PreloadCancelPayload(targetDim));

        source.sendSuccess(() -> Component.literal("[Auralith][debug] Released preload for " + targetDim.location() + " (server ticket + client hint sent)"), false);
        return 1;
    }
}