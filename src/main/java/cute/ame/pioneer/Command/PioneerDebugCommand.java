package cute.ame.pioneer.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cute.ame.pioneer.Core.Observer.ObserverState;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.Core.Observer.PlanetCube;
import cute.ame.pioneer.Core.Render.Debug.ShadingDebugMode;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PioneerDebugCommand
{
    private static final String PREFIX = ChatFormatting.DARK_GRAY + "[" + ChatFormatting.AQUA + "Pioneer" + ChatFormatting.DARK_GRAY + "][" + ChatFormatting.YELLOW + "debug" + ChatFormatting.DARK_GRAY + "] " + ChatFormatting.RESET;
    private static final String ERROR_PREFIX = ChatFormatting.DARK_GRAY + "[" + ChatFormatting.AQUA + "Pioneer" + ChatFormatting.DARK_GRAY + "][" + ChatFormatting.RED + "error" + ChatFormatting.DARK_GRAY + "] " + ChatFormatting.RESET;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("pdb")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("shading-debug")
                .executes(PioneerDebugCommand::executeShadingDebugQuery).then(Commands.literal("list")
                .executes(PioneerDebugCommand::executeShadingDebugList)).then(Commands.literal("cycle")
                .executes(PioneerDebugCommand::executeShadingDebugCycle)).then(Commands.argument("mode", StringArgumentType.word())
                .suggests(SHADING_MODES)
                .executes(PioneerDebugCommand::executeShadingDebugSet)))

            .then(Commands.literal("observer")
                .executes(PioneerDebugCommand::executeObserver))
        );
    }

    private static final SuggestionProvider<CommandSourceStack> SHADING_MODES = (ctx, builder) -> SharedSuggestionProvider.suggest(ShadingDebugMode.keys(), builder);

    private static int executeObserver(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        PlanetDefinition b = ObserverStates.surfaceBodyOf(p.level());
        ObserverState o = ObserverStates.resolve(p.level(), p.position(), 0f);
        if (!o.hasBody())
        {
            ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.GRAY + "origin=" + ChatFormatting.RED + "SPACE " + ChatFormatting.GRAY + "(%s)", p.level().dimension().location())), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(
            ChatFormatting.GRAY + "face=" + ChatFormatting.GREEN + "%d " +
            ChatFormatting.GRAY + "u=" + ChatFormatting.AQUA + "%.4f " +
            ChatFormatting.GRAY + "v=" + ChatFormatting.AQUA + "%.4f " +
            ChatFormatting.GRAY + "alt=" + ChatFormatting.YELLOW + "%.3fkm " +
            ChatFormatting.DARK_GRAY + "| " +
            ChatFormatting.GRAY + "lat=" + ChatFormatting.LIGHT_PURPLE + "%.2f " +
            ChatFormatting.GRAY + "lon=" + ChatFormatting.LIGHT_PURPLE + "%.2f " +
            ChatFormatting.DARK_GRAY + "| " +
            ChatFormatting.GRAY + "body=" + ChatFormatting.DARK_GRAY + "(" + ChatFormatting.RED + "%.1f" + ChatFormatting.DARK_GRAY + ", " + ChatFormatting.GREEN + "%.1f" + ChatFormatting.DARK_GRAY + ", " + ChatFormatting.AQUA + "%.1f" + ChatFormatting.DARK_GRAY + ") " +
            ChatFormatting.DARK_GRAY + "| " +
            ChatFormatting.GRAY + "halfSide=" + ChatFormatting.GOLD + "%.0f blocs",
            o.face(), o.u(), o.v(), o.altitudeKm(), o.latDeg(), o.lonDeg(), o.bodyKmX(), o.bodyKmY(), o.bodyKmZ(), PlanetCube.halfSide(b)
        )), false);
        return 1;
    }

    private static int executeShadingDebugQuery(CommandContext<CommandSourceStack> ctx)
    {
        ShadingDebugMode mode = ShadingDebugMode.current();
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "shading debug = " + ChatFormatting.GREEN + "%s " + ChatFormatting.GRAY + "(%s)", mode.key(), mode.description())), false);
        return 1;
    }

    private static int executeShadingDebugList(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "shading debug modes:"), false);
        for (ShadingDebugMode mode : ShadingDebugMode.values()) source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.YELLOW + "%s " + ChatFormatting.DARK_GRAY + "- " + ChatFormatting.GRAY + "%s", mode.key(), mode.description())), false);

        return ShadingDebugMode.values().length;
    }

    private static int executeShadingDebugCycle(CommandContext<CommandSourceStack> ctx)
    {
        ShadingDebugMode mode = ShadingDebugMode.cycle();
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "shading debug = " + ChatFormatting.GREEN + "%s " + ChatFormatting.GRAY + "(%s)", mode.key(), mode.description())), false);
        return 1;
    }

    private static int executeShadingDebugSet(CommandContext<CommandSourceStack> ctx)
    {
        String key = StringArgumentType.getString(ctx, "mode");
        var parsed = ShadingDebugMode.byKey(key);
        if (parsed.isEmpty())
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + String.format(ChatFormatting.RED + "unknown shading debug mode '" + ChatFormatting.DARK_RED + "%s" + ChatFormatting.RED + "' — try: " + ChatFormatting.YELLOW + "shading-debug list", key)));
            return 0;
        }

        ShadingDebugMode mode = parsed.get();
        ShadingDebugMode.set(mode);
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "shading debug = " + ChatFormatting.GREEN + "%s " + ChatFormatting.GRAY + "(%s)", mode.key(), mode.description())), false);
        return 1;
    }
}