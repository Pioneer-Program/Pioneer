package cute.ame.pioneer.Command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import cute.ame.pioneer.SkyPlanet.Physics.WorldClock;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.ERROR_PREFIX;
import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class TimeDebugCommand
{
    private static final double ERROR_TOLERANCE = 0.02;

    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("time")
            .executes(TimeDebugCommand::executeNow)
            .then(Commands.literal("scan").executes(ctx -> scan(ctx, 24))
                .then(Commands.argument("steps", IntegerArgumentType.integer(4, 96)).executes(ctx -> scan(ctx, IntegerArgumentType.getInteger(ctx, "steps")))))
            .then(Commands.literal("year").executes(ctx -> scanYear(ctx, 12))
                .then(Commands.argument("steps", IntegerArgumentType.integer(4, 48)).executes(ctx -> scanYear(ctx, IntegerArgumentType.getInteger(ctx, "steps")))));
    }

    private static int executeNow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<WorldClock.Sample> opt = WorldClock.sample(player.level(), 0.0);
        if (opt.isEmpty()) return noHost(ctx, player);

        WorldClock.Sample s = opt.get();
        float vanillaSkyAngle = player.level().getTimeOfDay(0f);
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%s " + ChatFormatting.GRAY + "| gameTime=" + ChatFormatting.GOLD + "%d " + ChatFormatting.GRAY + "| dayTime=" + ChatFormatting.DARK_GRAY + "%d " + ChatFormatting.DARK_GRAY + "(ignored)", s.body().id(), s.tick(), player.level().dayTime())), false);
        source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.GRAY + "our fraction=" + ChatFormatting.AQUA + "%.5f " + ChatFormatting.GRAY + "getTimeOfDay=" + ChatFormatting.AQUA + "%.5f " + ChatFormatting.GRAY + "local time=" + ChatFormatting.YELLOW + "%s", s.dayFraction(), vanillaSkyAngle, clockText(s.dayFraction()))), false);
        source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.GRAY + "sun up=" + ChatFormatting.GREEN + "%+.4f " + ChatFormatting.GRAY + "east=" + ChatFormatting.GREEN + "%+.4f " + ChatFormatting.GRAY + "north=" + ChatFormatting.GREEN + "%+.4f " + ChatFormatting.GRAY + "hourAngle=" + ChatFormatting.GREEN + "%+.1f deg", s.sunUp(), s.sunEast(), s.sunNorth(), Math.toDegrees(s.hourAngle()))), false);
        reportError(source, s.sunUp(), s.vanillaSunUp(), s.error());
        return 1;
    }

    private static int scan(CommandContext<CommandSourceStack> ctx, int steps) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<PlanetDefinition> hostOpt = WorldClock.surfaceHost(player.level());
        if (hostOpt.isEmpty()) return noHost(ctx, player);

        PlanetDefinition body = hostOpt.get();
        long period = (long) Math.max(Math.abs(body.axialRotationSpeed()) * 24000.0, 1.0);
        long start = player.level().getGameTime();

        header(ctx, body, "one rotation", period, steps);
        return sweep(ctx, body, start, period, steps);
    }

    private static int scanYear(CommandContext<CommandSourceStack> ctx, int steps) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<PlanetDefinition> hostOpt = WorldClock.surfaceHost(player.level());
        if (hostOpt.isEmpty()) return noHost(ctx, player);

        PlanetDefinition body = hostOpt.get();
        long period = (long) Math.max(body.orbit().periodDays() * 24000.0, 1.0);
        long start = player.level().getGameTime();

        header(ctx, body, "one orbit", period, steps);
        return sweep(ctx, body, start, period, steps);
    }

    private static void header(CommandContext<CommandSourceStack> ctx, PlanetDefinition body, String what, long period, int steps)
    {
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%s " + ChatFormatting.GRAY + "| %s = " + ChatFormatting.GOLD + "%d " + ChatFormatting.GRAY + "ticks | face " + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "| tilt " + ChatFormatting.GREEN + "%.1f deg " + ChatFormatting.GRAY + "| %d samples", body.id(), what, period, body.homeFace(), body.axialTilt(), steps)), false);

        ctx.getSource().sendSuccess(() -> Component.literal("  " + ChatFormatting.DARK_GRAY + "offset    frac     ours     vanilla   err"), false);
    }

    private static int sweep(CommandContext<CommandSourceStack> ctx, PlanetDefinition body, long start, long period, int steps)
    {
        CommandSourceStack source = ctx.getSource();
        double worst = 0.0;

        for (int i = 0; i < steps; i++)
        {
            long offset = period * i / steps;
            WorldClock.Sample s = WorldClock.sample(body, body.homeFace(), start + offset, 0.0);
            worst = Math.max(worst, Math.abs(s.error()));

            ChatFormatting colour = Math.abs(s.error()) <= ERROR_TOLERANCE ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
            final long off = offset;
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.DARK_GRAY + "%-9d " + ChatFormatting.AQUA + "%.4f  " + ChatFormatting.GREEN + "%+.4f  " + ChatFormatting.YELLOW + "%+.4f  " + colour + "%+.4f", off, s.dayFraction(), s.sunUp(), s.vanillaSunUp(), s.error())), false);
        }

        final double worstError = worst;
        source.sendSuccess(() -> Component.literal(PREFIX + (worstError <= ERROR_TOLERANCE
                ? ChatFormatting.GREEN + "clock coherent, worst error " + String.format("%.5f", worstError)
                : ChatFormatting.RED + "OUT OF SYNC, worst error " + String.format("%.5f", worstError)
                  + ChatFormatting.GRAY + " - light and drawn sky disagree")), false);
        return 1;
    }

    private static String clockText(double fraction)
    {
        double hours = ((fraction * 24.0) + 12.0) % 24.0;
        int h = (int) hours;
        int m = (int) ((hours - h) * 60.0);
        return String.format("%02d:%02d", h, m);
    }

    private static void reportError(CommandSourceStack source, double ours, double vanilla, double error)
    {
        boolean ok = Math.abs(error) <= ERROR_TOLERANCE;
        source.sendSuccess(() -> Component.literal("  " + (ok
                ? ChatFormatting.GREEN + "in sync " + ChatFormatting.DARK_GRAY + String.format("(err %+.5f)", error)
                : ChatFormatting.RED + String.format("OUT OF SYNC: ours %+.4f vs vanilla %+.4f, err %+.4f", ours, vanilla, error))), false);
    }

    private static int noHost(CommandContext<CommandSourceStack> ctx, ServerPlayer player)
    {
        ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED
                + "no surface body bound to " + player.level().dimension().location()
                + ChatFormatting.GRAY + " - the clock only has meaning on a surface"));
        return 0;
    }
}