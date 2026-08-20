package cute.ame.pioneer.Command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cute.ame.pioneer.Core.Render.Debug.ShadingDebugMode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.ERROR_PREFIX;
import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class ShadingDebugCommand
{
    private static final SuggestionProvider<CommandSourceStack> MODES = (ctx, builder) -> SharedSuggestionProvider.suggest(ShadingDebugMode.keys(), builder);

    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("shading-debug")
            .executes(ShadingDebugCommand::executeQuery)
            .then(Commands.literal("list").executes(ShadingDebugCommand::executeList))
            .then(Commands.literal("cycle").executes(ShadingDebugCommand::executeCycle))
            .then(Commands.argument("mode", StringArgumentType.word()).suggests(MODES).executes(ShadingDebugCommand::executeSet));
    }

    private static int executeQuery(CommandContext<CommandSourceStack> ctx)
    {
        ShadingDebugMode mode = ShadingDebugMode.current();
        report(ctx, mode);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "shading debug modes:"), false);
        for (ShadingDebugMode mode : ShadingDebugMode.values())
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.YELLOW + "%s " + ChatFormatting.DARK_GRAY + "- " + ChatFormatting.GRAY + "%s", mode.key(), mode.description())), false);
        return 1;
    }

    private static int executeCycle(CommandContext<CommandSourceStack> ctx)
    {
        report(ctx, ShadingDebugMode.cycle());
        return 1;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx)
    {
        String key = StringArgumentType.getString(ctx, "mode");
        Optional<ShadingDebugMode> mode = ShadingDebugMode.byKey(key);
        if (mode.isEmpty())
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + String.format(ChatFormatting.RED + "unknown shading debug mode '" + ChatFormatting.DARK_RED + "%s" + ChatFormatting.RED + "' - try: " + ChatFormatting.YELLOW + "shading-debug list", key)));
            return 0;
        }

        ShadingDebugMode.set(mode.orElse(null));
        report(ctx, mode.orElse(null));
        return 1;
    }

    private static void report(CommandContext<CommandSourceStack> ctx, ShadingDebugMode mode)
    {
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "shading debug = " + ChatFormatting.GREEN + "%s " + ChatFormatting.GRAY + "(%s)", mode.key(), mode.description())), false);
    }
}