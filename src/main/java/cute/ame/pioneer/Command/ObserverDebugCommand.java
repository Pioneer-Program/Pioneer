package cute.ame.pioneer.Command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cute.ame.pioneer.Core.Observer.CubeSurface;
import cute.ame.pioneer.Core.Observer.ObserverState;
import cute.ame.pioneer.Core.Observer.ObserverStates;
import cute.ame.pioneer.Core.Observer.PlanetCube;
import cute.ame.pioneer.SkyPlanet.Data.PlanetDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.ERROR_PREFIX;
import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class ObserverDebugCommand
{
    private static final String[] FACE_KEYS = { "front", "back", "left", "right", "top", "bottom" };
    private static final String[] FACE_AXES = { "+Z", "-Z", "-X", "+X", "+Y", "-Y" };

    private static final SuggestionProvider<CommandSourceStack> FACES = (ctx, builder) -> SharedSuggestionProvider.suggest(FACE_KEYS, builder);

    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("observer")
            .executes(ObserverDebugCommand::executeState)
            .then(Commands.literal("faces").executes(ObserverDebugCommand::executeFaces))
            .then(Commands.literal("tp")
                .then(Commands.argument("face", StringArgumentType.word()).suggests(FACES).executes(ObserverDebugCommand::executeTeleportCentre)
                    .then(Commands.argument("u", DoubleArgumentType.doubleArg(-2.0, 2.0))
                        .then(Commands.argument("v", DoubleArgumentType.doubleArg(-2.0, 2.0)).executes(ObserverDebugCommand::executeTeleport)))));
    }

    private static int executeState(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlanetDefinition body = ObserverStates.surfaceBodyOf(player.level());
        ObserverState state = ObserverStates.resolve(player.level(), player.position(), 0f);

        if (!state.hasBody())
        {
            ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.GRAY + "origin=" + ChatFormatting.RED + "SPACE " + ChatFormatting.GRAY + "(%s)", player.level().dimension().location())), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(
            ChatFormatting.GRAY + "face=" + ChatFormatting.GREEN + "%s " +
            ChatFormatting.GRAY + "u=" + ChatFormatting.AQUA + "%.4f " +
            ChatFormatting.GRAY + "v=" + ChatFormatting.AQUA + "%.4f " +
            ChatFormatting.GRAY + "alt=" + ChatFormatting.YELLOW + "%.3fkm " +
            ChatFormatting.DARK_GRAY + "| " +
            ChatFormatting.GRAY + "lat=" + ChatFormatting.LIGHT_PURPLE + "%.2f " +
            ChatFormatting.GRAY + "lon=" + ChatFormatting.LIGHT_PURPLE + "%.2f " +
            ChatFormatting.DARK_GRAY + "| " +
            ChatFormatting.GRAY + "body=" + ChatFormatting.DARK_GRAY + "(" + ChatFormatting.RED + "%.1f" + ChatFormatting.DARK_GRAY + ", " + ChatFormatting.GREEN + "%.1f" + ChatFormatting.DARK_GRAY + ", " + ChatFormatting.AQUA + "%.1f" + ChatFormatting.DARK_GRAY + ") " +
            ChatFormatting.DARK_GRAY + "| " +
            ChatFormatting.GRAY + "halfSide=" + ChatFormatting.GOLD + "%.0f blocks",

            faceLabel(state.face()), state.u(), state.v(), state.altitudeKm(), state.latDeg(), state.lonDeg(), state.bodyKmX(), state.bodyKmY(), state.bodyKmZ(), PlanetCube.halfSide(body)
        )), false);

        warnIfOutOfBounds(ctx, state, body);
        return 1;
    }

    private static void warnIfOutOfBounds(CommandContext<CommandSourceStack> ctx, ObserverState state, PlanetDefinition body)
    {
        if (!state.outOfFaceBounds()) return;

        CubeSurface.Crossing crossing = CubeSurface.cross(PlanetCube.halfSide(body), state.face(), state.u(), state.v());
        String detail = crossing == null ? ChatFormatting.DARK_RED + "corner, or too far out, cross() refuses" : String.format(ChatFormatting.YELLOW + "should cross onto %s at x=%.0f z=%.0f", faceLabel(crossing.face()), crossing.blockX(), crossing.blockZ());
        ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + String.format(ChatFormatting.RED + "OFF FACE: u=%.3f v=%.3f - ", state.u(), state.v()) + detail));
    }

    private static int executeFaces(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlanetDefinition body = ObserverStates.surfaceBodyOf(player.level());
        if (body == null) return noBody(ctx, player);

        double halfSide = PlanetCube.halfSide(body);
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%s " + ChatFormatting.GRAY + "| halfSide=" + ChatFormatting.GOLD + "%.0f " + ChatFormatting.GRAY + "| face=" + ChatFormatting.GOLD + "%.0f" + ChatFormatting.GRAY + " blocks across", body.id(), halfSide, halfSide * 2.0)), false);

        for (int i = 0; i < FACE_KEYS.length; i++)
        {
            final int face = i;
            double centre = PlanetCube.faceOriginX(body, face);
            source.sendSuccess(() -> Component.literal(String.format(
                "  " + ChatFormatting.GREEN + "%-7s " + ChatFormatting.DARK_GRAY + "%-3s "
                + ChatFormatting.GRAY + "centre x=" + ChatFormatting.YELLOW + "%.0f "
                + ChatFormatting.GRAY + "edges " + ChatFormatting.DARK_GRAY + "[" + ChatFormatting.AQUA + "%.0f"
                + ChatFormatting.DARK_GRAY + " .. " + ChatFormatting.AQUA + "%.0f" + ChatFormatting.DARK_GRAY + "]",
                FACE_KEYS[face], FACE_AXES[face], centre, centre - halfSide, centre + halfSide)), false);
        }
        return 1;
    }

    private static int executeTeleportCentre(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        return teleport(ctx, 0.0, 0.0);
    }

    private static int executeTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        return teleport(ctx, DoubleArgumentType.getDouble(ctx, "u"), DoubleArgumentType.getDouble(ctx, "v"));
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx, double u, double v) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlanetDefinition body = ObserverStates.surfaceBodyOf(player.level());
        if (body == null) return noBody(ctx, player);

        String key = StringArgumentType.getString(ctx, "face");
        int face = faceIndexOf(key);
        if (face < 0)
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "unknown face '" + ChatFormatting.DARK_RED + key + ChatFormatting.RED + "' - front, back, left, right, top, bottom"));
            return 0;
        }

        double halfSide = PlanetCube.halfSide(body);
        double x = PlanetCube.faceOriginX(body, face) + u * halfSide;
        double z = v * halfSide;

        ServerLevel level = (ServerLevel) player.level();
        level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) + 2;
        player.teleportTo(level, x + 0.5, y, z + 0.5, Set.of(), player.getYRot(), player.getXRot());

        String warning = (Math.abs(u) > 1.0 || Math.abs(v) > 1.0) ? ChatFormatting.RED + " [OFF FACE]" : "";
        final int landedY = y;
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.GRAY + "face " + ChatFormatting.GREEN + "%s " + ChatFormatting.GRAY + "u=" + ChatFormatting.AQUA + "%.3f " + ChatFormatting.GRAY + "v=" + ChatFormatting.AQUA + "%.3f " + ChatFormatting.DARK_GRAY + "-> " + ChatFormatting.YELLOW + "%.0f %d %.0f%s", faceLabel(face), u, v, x, landedY, z, warning)), false);
        return 1;
    }

    private static int noBody(CommandContext<CommandSourceStack> ctx, ServerPlayer player)
    {
        ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "no surface body bound to " + player.level().dimension().location()));
        return 0;
    }

    private static int faceIndexOf(String key)
    {
        for (int i = 0; i < FACE_KEYS.length; i++) if (FACE_KEYS[i].equalsIgnoreCase(key)) return i;

        return -1;
    }

    private static String faceLabel(int face)
    {
        return FACE_KEYS[face] + " " + FACE_AXES[face];
    }
}