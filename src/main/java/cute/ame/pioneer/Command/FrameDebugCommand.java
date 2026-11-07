package cute.ame.pioneer.Command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cute.ame.pioneer.Core.API.Frame.FrameAPI;
import cute.ame.pioneer.Core.API.Frame.FrameError;
import cute.ame.pioneer.Core.API.Frame.FrameReport;
import cute.ame.pioneer.Core.API.Frame.FrameResult;
import cute.ame.pioneer.Core.Frame.FrameBox;
import cute.ame.pioneer.Core.Frame.FrameGrid;
import cute.ame.pioneer.Core.Frame.FrameMotion;
import cute.ame.pioneer.Core.Frame.FramePose;
import cute.ame.pioneer.Core.Frame.FrameQuat;
import cute.ame.pioneer.Frame.FrameBodies;
import cute.ame.pioneer.Frame.LocalFrame;
import cute.ame.pioneer.SkyPlanet.Data.SolarSystemDefinition;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.ERROR_PREFIX;
import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class FrameDebugCommand
{
    private static final double SHIP_REACH = 2.0, DEFAULT_ALTITUDE_KM = 400.0, DEFAULT_NEAR = 10000.0;
    private static final DynamicCommandExceptionType NOT_SPACE = new DynamicCommandExceptionType(dimension -> Component.literal(ERROR_PREFIX + ChatFormatting.RED + "frames only exist in a space dimension, not " + ChatFormatting.DARK_RED + dimension));
    private static final SimpleCommandExceptionType NOT_IN_FRAME = new SimpleCommandExceptionType(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "you are not in a frame"));

    private static final SuggestionProvider<CommandSourceStack> BODIES = (ctx, builder) ->
    {
        return SharedSuggestionProvider.suggest(FrameAPI.bodies(ctx.getSource().getLevel()).stream().map(FrameBodies.Body::name), builder);
    };

    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("frame")
            .executes(FrameDebugCommand::executeInfo)
            .then(Commands.literal("list").executes(FrameDebugCommand::executeList))
            .then(Commands.literal("grid").executes(FrameDebugCommand::executeGrid))
            .then(Commands.literal("create").executes(ctx -> executeCreate(ctx, null))
                .then(Commands.argument("body", StringArgumentType.word()).suggests(BODIES)
                    .executes(ctx -> executeCreate(ctx, StringArgumentType.getString(ctx, "body")))))
            .then(Commands.literal("release").executes(ctx -> executeRelease(ctx, false))
                .then(Commands.literal("force").executes(ctx -> executeRelease(ctx, true))))
            .then(Commands.literal("open").executes(FrameDebugCommand::executeOpen))
            .then(Commands.literal("join")
                .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(FrameDebugCommand::executeJoin)))
            .then(Commands.literal("recenter").executes(FrameDebugCommand::executeRecenter))
            .then(Commands.literal("push")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(FrameDebugCommand::executePush)))))
            .then(Commands.literal("accel")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(FrameDebugCommand::executeAccel)))))
            .then(Commands.literal("stop").executes(FrameDebugCommand::executeStop))
            .then(Commands.literal("place")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(FrameDebugCommand::executePlace)))))
            .then(Commands.literal("above")
                .then(Commands.argument("body", StringArgumentType.word()).suggests(BODIES)
                    .executes(ctx -> executeAbove(ctx, DEFAULT_ALTITUDE_KM))
                    .then(Commands.argument("km", DoubleArgumentType.doubleArg(0.0)).executes(ctx -> executeAbove(ctx, DoubleArgumentType.getDouble(ctx, "km"))))))
            .then(Commands.literal("orient")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("degrees", DoubleArgumentType.doubleArg()).executes(FrameDebugCommand::executeOrient))))))
            .then(Commands.literal("near").executes(ctx -> executeNear(ctx, DEFAULT_NEAR))
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0)).executes(ctx -> executeNear(ctx, DoubleArgumentType.getDouble(ctx, "radius")))))
            .then(Commands.literal("drop")
                .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(FrameDebugCommand::executeDrop)))
            .then(Commands.literal("parent")
                .then(Commands.literal("none").executes(ctx -> executeParent(ctx, null, false)))
                .then(Commands.argument("body", StringArgumentType.word()).suggests(BODIES)
                    .executes(ctx -> executeParent(ctx, StringArgumentType.getString(ctx, "body"), false))
                    .then(Commands.literal("fixed").executes(ctx -> executeParent(ctx, StringArgumentType.getString(ctx, "body"), true)))))
            .then(Commands.literal("spin")
                .then(Commands.literal("reset").executes(FrameDebugCommand::executeUnspin))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(FrameDebugCommand::executeSpin)))))

            ;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        ServerPlayer player = space.player();
        CommandSourceStack source = ctx.getSource();
        long now = space.level().getGameTime();
        LocalFrame frame = FrameAPI.frameOf(player);

        if (frame == null)
        {
            FrameGrid grid = FrameAPI.grid(space.level());
            int cell = grid.cellAt(player.getX(), player.getZ());
            String where = cell < 0 ? ChatFormatting.RED + "outside grid" : String.format(ChatFormatting.GREEN + "free (%d,%d)", grid.ix(cell), grid.iz(cell));
            source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "not in a frame " + ChatFormatting.GRAY + "cell=" + where), false);
            source.sendSuccess(() -> Component.literal("  " + xyz("raw", player.getX(), player.getY(), player.getZ())), false);
            source.sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), player.getX(), player.getY(), player.getZ())), false);
            return 1;
        }

        FrameAPI.refresh(space.level());
        double[] virtual = FrameAPI.toSystem(space.level(), frame, player.getX(), player.getY(), player.getZ(), now, 0.0, new double[3]);

        source.sendSuccess(() -> Component.literal(PREFIX + frameText(frame, FrameAPI.grid(space.level()))), false);
        source.sendSuccess(() -> Component.literal("  " + xyz("virtual", virtual[0], virtual[1], virtual[2]) + " " + xyz("raw", player.getX(), player.getY(), player.getZ()) + " " + xyz("offset", player.getX() - FrameAPI.grid(space.level()).centerX(frame.cell()), player.getY() - FrameAPI.grid(space.level()).centerY(), player.getZ() - FrameAPI.grid(space.level()).centerZ(frame.cell()))), false);
        source.sendSuccess(() -> Component.literal("  " + motionText(frame.motion(), now)), false);
        source.sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), virtual[0], virtual[1], virtual[2])), false);
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        CommandSourceStack source = ctx.getSource();
        FrameGrid grid = FrameAPI.grid(space.level());
        List<LocalFrame> frames = FrameAPI.frames(space.level());
        int count = frames.size();
        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d frame(s) " + ChatFormatting.GRAY + "| grid=" + ChatFormatting.AQUA + "%dx%d " + ChatFormatting.GRAY + "spacing=" + ChatFormatting.GOLD + "%d " + ChatFormatting.GRAY + "used=" + ChatFormatting.GOLD + "%d/%d", count, grid.side(), grid.side(), grid.spacing(), count, grid.cellCount())), false);
        if (count == 0) return 1;

        FrameAPI.refresh(space.level());
        long now = space.level().getGameTime();
        double[] position = new double[3];

        for (LocalFrame frame : frames)
        {
            FrameAPI.origin(space.level(), frame, now, 0.0, position);
            final String line = "  " + frameText(frame, grid) + " " + ChatFormatting.GRAY + "speed=" + ChatFormatting.YELLOW + String.format("%.1f u/s ", frame.motion().speedAt(now) * 20.0) + xyz("virtual", position[0], position[1], position[2]);
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int executeGrid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        ServerPlayer player = space.player();
        CommandSourceStack source = ctx.getSource();
        FrameGrid grid = FrameAPI.grid(space.level());
        FrameAPI.refresh(space.level());

        int used = FrameAPI.frames(space.level()).size();
        String precise = grid.precise() ? ChatFormatting.GREEN + "precise" : ChatFormatting.RED + "PAST 32768";
        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "grid %dx%d " + ChatFormatting.GRAY + "spacing=" + ChatFormatting.GOLD + "%d " + ChatFormatting.GRAY + "extent=" + ChatFormatting.AQUA + "+-%.0f " + ChatFormatting.GRAY + "y=" + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "used=" + ChatFormatting.GOLD + "%d/%d " + ChatFormatting.DARK_GRAY + "(%s" + ChatFormatting.DARK_GRAY + ")", grid.side(), grid.side(), grid.spacing(), grid.extent(), grid.y(), used, grid.cellCount(), precise)), false);

        int mine = grid.cellAt(player.getX(), player.getZ());
        int radius = grid.radius();
        for (int iz = -radius; iz <= radius; iz++)
        {
            StringBuilder row = new StringBuilder(8 + 3 * grid.side());
            row.append("  ").append(ChatFormatting.GRAY).append(String.format("z%+d ", iz));

            for (int ix = -radius; ix <= radius; ix++)
            {
                int cell = grid.cell(ix, iz);
                LocalFrame frame = FrameAPI.frameInCell(space.level(), cell);

                String token;
                if (frame != null) token = (cell == mine ? "@" : "") + frame.id;
                else if (cell == mine) token = "@";
                else if (FrameAPI.strays(space.level(), cell) > 0) token = "*";
                else token = ".";
                ChatFormatting colour = frame != null ? ChatFormatting.AQUA : cell == mine ? ChatFormatting.YELLOW : token.equals("*") ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY;
                row.append(colour).append(String.format("%3s", token));
            }

            final String line = row.toString();
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int executeCreate(CommandContext<CommandSourceStack> ctx, @Nullable String bodyName) throws CommandSyntaxException
    {
        Space space = space(ctx);
        CommandSourceStack source = ctx.getSource();
        FrameBodies.Body body = null;

        if (bodyName != null)
        {
            if (space.system() == null)
            {
                source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "no solar system bound to " + space.level().dimension().location()));
                return 0;
            }

            body = FrameAPI.body(space.level(), bodyName);
            if (body == null)
            {
                source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "unknown body '" + ChatFormatting.DARK_RED + bodyName + ChatFormatting.RED + "' - try: " + ChatFormatting.YELLOW + FrameAPI.bodies(space.level()).stream().map(FrameBodies.Body::name).collect(Collectors.joining(", "))));
                return 0;
            }
        }

        ServerPlayer player = space.player();
        ServerSubLevel ship = shipOf(player, space.level());
        if (ship == null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "no ship under you - stand on one (within " + (int) SHIP_REACH + " blocks)"));
            return 0;
        }

        FrameResult created;
        try
        {
            created = FrameAPI.create(space.level(), player, ship, body);
        }
        catch (FrameError e)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + e.getMessage()));
            return 0;
        }

        LocalFrame frame = created.frame();
        var report = created.report();
        long now = space.level().getGameTime();
        double[] virtual = FrameAPI.toSystem(space.level(), frame, player.getX(), player.getY(), player.getZ(), now, 0.0, new double[3]);

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GREEN + "created " + frameText(frame, FrameAPI.grid(space.level()))), false);
        source.sendSuccess(() -> Component.literal("  " + String.format(ChatFormatting.GRAY + "carried sublevels=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "players=" + ChatFormatting.GREEN + "%d " + ChatFormatting.DARK_GRAY + "(riding %d, seated %d, free %d) " + ChatFormatting.GRAY + "entities=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "tick=" + ChatFormatting.DARK_GRAY + "%d", report.subLevels, report.players(), report.riders, report.seated, report.free, report.entities, report.tick)), false);
        source.sendSuccess(() -> Component.literal("  " + xyz("virtual", virtual[0], virtual[1], virtual[2]) + " " + xyz("raw", player.getX(), player.getY(), player.getZ())), false);
        source.sendSuccess(() -> Component.literal("  " + motionText(frame.motion(), now)), false);
        source.sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), virtual[0], virtual[1], virtual[2])), false);
        return 1;
    }

    private static int executeRelease(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException
    {
        Space space = space(ctx);
        ServerPlayer player = space.player();
        CommandSourceStack source = ctx.getSource();
        LocalFrame frame = FrameAPI.frameOf(player);
        if (frame == null) throw NOT_IN_FRAME.create();

        FrameResult released;
        try
        {
            released = FrameAPI.release(space.level(), frame, force);
        }
        catch (FrameError e)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + e.getMessage()));
            return 0;
        }

        FrameGrid grid = FrameAPI.grid(space.level());
        var report = released.report();
        int id = released.frame().id;
        int cell = released.frame().cell();
        long now = space.level().getGameTime();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.GREEN + "released " + ChatFormatting.WHITE + "frame #%d " + ChatFormatting.GRAY + "freed cell=" + ChatFormatting.AQUA + "(%d,%d)", id, grid.ix(cell), grid.iz(cell))), false);
        source.sendSuccess(() -> Component.literal("  " + String.format(ChatFormatting.GRAY + "moved sublevels=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "players=" + ChatFormatting.GREEN + "%d " + ChatFormatting.DARK_GRAY + "(riding %d, seated %d, free %d) " + ChatFormatting.GRAY + "entities=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "tick=" + ChatFormatting.DARK_GRAY + "%d", report.subLevels, report.players(), report.riders, report.seated, report.free, report.entities, report.tick)), false);
        source.sendSuccess(() -> Component.literal("  " + xyz("raw", player.getX(), player.getY(), player.getZ())), false);
        source.sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), player.getX(), player.getY(), player.getZ())), false);
        return 1;
    }

    private static int executeOpen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        ServerPlayer player = space.player();
        CommandSourceStack source = ctx.getSource();
        LocalFrame own = FrameAPI.frameOf(player);
        if (own != null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "you are already in frame #" + own.id));
            return 0;
        }

        long now = space.level().getGameTime();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        LocalFrame frame;
        try
        {
            frame = FrameAPI.open(space.level(), new FrameMotion().set(now, x, y, z, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0), null, false, false);
        }
        catch (FrameError e)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + e.getMessage()));
            return 0;
        }

        FrameGrid grid = FrameAPI.grid(space.level());
        player.teleportTo(space.level(), grid.centerX(frame.cell()), grid.centerY(), grid.centerZ(frame.cell()), Set.of(), player.getYRot(), player.getXRot());
        FrameAPI.refresh(space.level());

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GREEN + "opened " + frameText(frame, grid)), false);
        source.sendSuccess(() -> Component.literal("  " + xyz("virtual", x, y, z) + " " + xyz("raw", player.getX(), player.getY(), player.getZ())), false);
        source.sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), x, y, z)), false);
        return 1;
    }

    private static int executePlace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        double x = DoubleArgumentType.getDouble(ctx, "x"), y = DoubleArgumentType.getDouble(ctx, "y"), z = DoubleArgumentType.getDouble(ctx, "z");
        long now = space.level().getGameTime();
        FrameAPI.place(space.level(), frame, x, y, z);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + "placed " + xyz("origin", x, y, z) + " " + ChatFormatting.GRAY + "| " + motionText(frame.motion(), now)), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), x, y, z)), false);
        return 1;
    }

    private static int executeAbove(CommandContext<CommandSourceStack> ctx, double km) throws CommandSyntaxException
    {
        Space space = space(ctx);
        CommandSourceStack source = ctx.getSource();
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        String bodyName = StringArgumentType.getString(ctx, "body");
        FrameBodies.Body body = FrameAPI.body(space.level(), bodyName);
        if (body == null || body.planet() == null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "'" + ChatFormatting.DARK_RED + bodyName + ChatFormatting.RED + "' is not a planet or a moon"));
            return 0;
        }

        double floor = body.planet().dimension().isPresent() ? FrameAPI.reentryAltitudeKm() : 0.0;
        if (km <= floor)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + String.format("%.1f km is under the re-entry altitude of %s (%.1f km), you would warp to the surface", km, body.name(), floor)));
            return 0;
        }

        long now = space.level().getGameTime();
        FrameMotion over = FrameAPI.surfaceMotion(body.planet(), 0, 0.0, 0.0, km, now, new FrameMotion());
        FramePose pose = new FramePose().set(over, body, true, now, 0.0);
        FrameAPI.assign(space.level(), frame, over, body, true);

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + String.format("held " + ChatFormatting.YELLOW + "%.1f km " + ChatFormatting.GRAY + "above " + ChatFormatting.AQUA + "%s " + ChatFormatting.GRAY + "parent=" + ChatFormatting.AQUA + "%s", km, body.name(), parentText(frame))), false);
        source.sendSuccess(() -> Component.literal("  " + motionText(frame.motion(), now)), false);
        source.sendSuccess(() -> Component.literal("  " + ChatFormatting.GRAY + FrameAPI.nearestBody(space.level(), pose.px, pose.py, pose.pz)), false);
        return 1;
    }

    private static int executeOrient(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        double x = DoubleArgumentType.getDouble(ctx, "x"), y = DoubleArgumentType.getDouble(ctx, "y"), z = DoubleArgumentType.getDouble(ctx, "z"), degrees = DoubleArgumentType.getDouble(ctx, "degrees");
        if (x * x + y * y + z * z < 1.0e-12)
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "the axis cannot be 0 0 0"));
            return 0;
        }

        long now = space.level().getGameTime();
        double[] q = FrameQuat.axisAngle(x, y, z, Math.toRadians(degrees), new double[4]);
        FrameAPI.orient(space.level(), frame, q[0], q[1], q[2], q[3]);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + "oriented | " + motionText(frame.motion(), now)), false);
        return 1;
    }

    private static int executeNear(CommandContext<CommandSourceStack> ctx, double radius) throws CommandSyntaxException
    {
        Space space = space(ctx);
        CommandSourceStack source = ctx.getSource();
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        long now = space.level().getGameTime();
        FrameGrid grid = FrameAPI.grid(space.level());
        FramePose mine = FrameAPI.pose(space.level(), frame, 0.0, new FramePose());
        double[] velocity = FrameAPI.velocity(space.level(), frame, now, new double[3]);
        double[] origin = new double[3], offset = new double[3], other = new double[3];
        int found = 0;

        FrameAPI.refresh(space.level());
        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frames within " + ChatFormatting.GOLD + String.format("%.0f", radius) + ChatFormatting.WHITE + " of frame #" + frame.id), false);
        for (LocalFrame f : FrameAPI.frames(space.level()))
        {
            if (f == frame) continue;

            FrameAPI.origin(space.level(), f, now, 0.0, origin);
            mine.toLocal(origin[0], origin[1], origin[2], offset);
            double distance = Math.sqrt(offset[0] * offset[0] + offset[1] * offset[1] + offset[2] * offset[2]);
            if (distance > radius) continue;

            FrameAPI.velocity(space.level(), f, now, other);
            double dx = other[0] - velocity[0], dy = other[1] - velocity[1], dz = other[2] - velocity[2];
            final String line = "  " + frameText(f, grid) + " " + ChatFormatting.GRAY + String.format("d=" + ChatFormatting.YELLOW + "%.1f " + ChatFormatting.GRAY + "dv=" + ChatFormatting.YELLOW + "%.1f u/s ", distance, Math.sqrt(dx * dx + dy * dy + dz * dz)) + xyz("offset", offset[0], offset[1], offset[2]);
            source.sendSuccess(() -> Component.literal(line), false);
            found++;
        }

        if (found == 0) source.sendSuccess(() -> Component.literal("  " + ChatFormatting.DARK_GRAY + "none"), false);

        return 1;
    }

    private static int executeJoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        ServerPlayer player = space.player();
        CommandSourceStack source = ctx.getSource();
        int id = IntegerArgumentType.getInteger(ctx, "id");
        LocalFrame frame = FrameAPI.frame(space.level(), id);
        if (frame == null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "unknown frame #" + id));
            return 0;
        }

        ServerSubLevel ship = shipOf(player, space.level());
        if (ship == null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "no ship under you - stand on one (within " + (int) SHIP_REACH + " blocks)"));
            return 0;
        }

        FrameReport report;
        try
        {
            report = FrameAPI.join(space.level(), frame, ship);
        }
        catch (FrameError e)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + e.getMessage()));
            return 0;
        }

        Vector3d local = FrameAPI.velocity(space.level(), ship, new Vector3d());
        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GREEN + "joined " + frameText(frame, FrameAPI.grid(space.level()))), false);
        source.sendSuccess(() -> Component.literal("  " + carriedText("carried", report)), false);
        source.sendSuccess(() -> Component.literal("  " + xyz("offset", player.getX() - FrameAPI.grid(space.level()).centerX(frame.cell()), player.getY() - FrameAPI.grid(space.level()).centerY(), player.getZ() - FrameAPI.grid(space.level()).centerZ(frame.cell())) + String.format(ChatFormatting.GRAY + " ship speed in frame=" + ChatFormatting.YELLOW + "%.1f u/s", local.length())), false);
        return 1;
    }

    private static int executeRecenter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        ServerPlayer player = space.player();
        CommandSourceStack source = ctx.getSource();
        LocalFrame frame = FrameAPI.frameOf(player);
        if (frame == null) throw NOT_IN_FRAME.create();

        long now = space.level().getGameTime();
        double[] before = FrameAPI.toSystem(space.level(), frame, player.getX(), player.getY(), player.getZ(), now, 0.0, new double[3]);
        FrameReport report;
        try
        {
            report = FrameAPI.recenter(space.level(), frame);
        }
        catch (FrameError e)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + e.getMessage()));
            return 0;
        }

        double[] after = FrameAPI.toSystem(space.level(), frame, player.getX(), player.getY(), player.getZ(), now, 0.0, new double[3]);
        double drift = Math.sqrt((after[0] - before[0]) * (after[0] - before[0]) + (after[1] - before[1]) * (after[1] - before[1]) + (after[2] - before[2]) * (after[2] - before[2]));
        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GREEN + "recentered " + frameText(frame, FrameAPI.grid(space.level()))), false);
        source.sendSuccess(() -> Component.literal("  " + carriedText("moved", report)), false);
        source.sendSuccess(() -> Component.literal("  " + motionText(frame.motion(), now) + String.format(ChatFormatting.GRAY + " | virtual drift=" + ChatFormatting.YELLOW + "%.4f", drift)), false);
        return 1;
    }

    private static String carriedText(String verb, FrameReport report)
    {
        return String.format(ChatFormatting.GRAY + verb + " sublevels=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "players=" + ChatFormatting.GREEN + "%d " + ChatFormatting.DARK_GRAY + "(riding %d, seated %d, free %d) " + ChatFormatting.GRAY + "entities=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "tick=" + ChatFormatting.DARK_GRAY + "%d", report.subLevels, report.players(), report.riders, report.seated, report.free, report.entities, report.tick);
    }

    private static int executePush(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        long now = space.level().getGameTime();
        FrameAPI.push(space.level(), frame, DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "y"), DoubleArgumentType.getDouble(ctx, "z"));

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + "pushed | " + motionText(frame.motion(), now)), false);
        return 1;
    }

    private static int executeAccel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        long now = space.level().getGameTime();
        FrameAPI.accelerate(space.level(), frame, DoubleArgumentType.getDouble(ctx, "x"), DoubleArgumentType.getDouble(ctx, "y"), DoubleArgumentType.getDouble(ctx, "z"));

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + "accelerating | " + motionText(frame.motion(), now)), false);
        return 1;
    }

    private static int executeStop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        long now = space.level().getGameTime();
        FrameAPI.stop(space.level(), frame);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + "stopped | " + motionText(frame.motion(), now)), false);
        return 1;
    }

    private static int executeSpin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        double x = DoubleArgumentType.getDouble(ctx, "x"), y = DoubleArgumentType.getDouble(ctx, "y"), z = DoubleArgumentType.getDouble(ctx, "z");
        long now = space.level().getGameTime();
        FrameAPI.spin(space.level(), frame, Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));

        double rate = Math.sqrt(x * x + y * y + z * z);
        String axis = rate < 1.0e-12 ? "-" : String.format("(%.2f, %.2f, %.2f)", x / rate, y / rate, z / rate);
        String detail = String.format(ChatFormatting.GRAY + "spinning " + ChatFormatting.YELLOW + "%.1f deg/s " + ChatFormatting.GRAY + "axis=" + ChatFormatting.AQUA + "%s " + ChatFormatting.GRAY + "tilt=" + ChatFormatting.YELLOW + "%.1f deg", rate, axis, tiltDegrees(frame.motion(), now));

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + detail), false);
        return 1;
    }

    private static int executeParent(CommandContext<CommandSourceStack> ctx, @Nullable String bodyName, boolean fixed) throws CommandSyntaxException
    {
        Space space = space(ctx);
        CommandSourceStack source = ctx.getSource();
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        FrameBodies.Body body = bodyName == null || space.system() == null ? null : FrameAPI.body(space.level(), bodyName);
        if (bodyName != null && body == null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "unknown body '" + ChatFormatting.DARK_RED + bodyName + ChatFormatting.RED + "'"));
            return 0;
        }

        long now = space.level().getGameTime();
        FrameAPI.reparent(space.level(), frame, body, fixed);

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + "parent=" + ChatFormatting.AQUA + parentText(frame) + ChatFormatting.GRAY + " | " + motionText(frame.motion(), now)), false);
        return 1;
    }

    private static String parentText(LocalFrame frame)
    {
        String text = frame.parentName().isEmpty() ? "none" : frame.parentName() + (frame.fixed() ? " (fixed)" : "");
        return frame.auto() ? text + " auto" : text;
    }

    private static int executeDrop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        CommandSourceStack source = ctx.getSource();
        int id = IntegerArgumentType.getInteger(ctx, "id");
        LocalFrame frame = FrameAPI.frame(space.level(), id);
        if (frame == null)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + "unknown frame #" + id));
            return 0;
        }

        try
        {
            FrameAPI.drop(space.level(), frame);
        }
        catch (FrameError e)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + ChatFormatting.RED + e.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GREEN + "dropped " + ChatFormatting.WHITE + "frame #" + id), false);
        return 1;
    }

    private static int executeUnspin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Space space = space(ctx);
        LocalFrame frame = FrameAPI.frameOf(space.player());
        if (frame == null) throw NOT_IN_FRAME.create();

        long now = space.level().getGameTime();
        double tilt = tiltDegrees(frame.motion(), now);
        FrameAPI.unspin(space.level(), frame);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "frame #" + frame.id + " " + ChatFormatting.GRAY + String.format("spin reset " + ChatFormatting.DARK_GRAY + "(was tilt=" + ChatFormatting.YELLOW + "%.1f deg" + ChatFormatting.DARK_GRAY + ")", tilt)), false);
        return 1;
    }

    private static String frameText(LocalFrame frame, FrameGrid grid)
    {
        ServerSubLevel anchor = frame.anchor();
        return String.format(ChatFormatting.WHITE + "frame #%d " + ChatFormatting.GRAY + "cell=" + ChatFormatting.AQUA + "(%d,%d) " + ChatFormatting.GRAY + "members=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "players=" + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "mass=" + ChatFormatting.GOLD + "%.1f " + ChatFormatting.GRAY + "anchor=" + ChatFormatting.DARK_GRAY + "%s " + ChatFormatting.GRAY + "parent=" + ChatFormatting.AQUA + "%s", frame.id, grid.ix(frame.cell()), grid.iz(frame.cell()), frame.memberCount(), frame.playerCount(), frame.mass(), anchor == null ? "-" : FrameAPI.name(anchor), parentText(frame));
    }

    private static String motionText(FrameMotion motion, long now)
    {
        String text = String.format(ChatFormatting.GRAY + "speed=" + ChatFormatting.YELLOW + "%.1f u/s " + ChatFormatting.GRAY + "accel=" + ChatFormatting.YELLOW + "%.1f u/s2", motion.speedAt(now) * 20.0, motion.accel() * 400.0);
        if (motion.rotated())
            text += String.format(ChatFormatting.GRAY + " spin=" + ChatFormatting.YELLOW + "%.1f deg/s " + ChatFormatting.GRAY + "tilt=" + ChatFormatting.YELLOW + "%.1f deg", Math.toDegrees(motion.spinRate()) * 20.0, tiltDegrees(motion, now));
        return text;
    }

    private static Space space(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        if (!FrameAPI.supports(level)) throw NOT_SPACE.create(level.dimension().location());

        return new Space(player, level, FrameAPI.system(level));
    }

    private static String xyz(String label, double x, double y, double z)
    {
        return String.format(ChatFormatting.GRAY + "%s=" + ChatFormatting.AQUA + "(%.2f, %.2f, %.2f)", label, x, y, z);
    }

    private static double tiltDegrees(FrameMotion motion, long now)
    {
        double[] q = motion.orientationAt(now, 0.0, new double[4]);
        return FrameQuat.angleDegrees(q[0], q[1], q[2], q[3]);
    }

    @Nullable
    private static ServerSubLevel shipOf(ServerPlayer player, ServerLevel level)
    {
        SubLevel direct = Sable.HELPER.getTrackingOrVehicleSubLevel(player);
        if (direct instanceof ServerSubLevel ship && !ship.isRemoved()) return ship;

        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) return null;

        AABB bounds = player.getBoundingBox();
        FrameBox probe = new FrameBox().set(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
        ServerSubLevel best = null;
        double bestGap = SHIP_REACH;

        for (ServerSubLevel candidate : container.getAllSubLevels())
        {
            if (candidate.isRemoved()) continue;

            BoundingBox3dc box = candidate.boundingBox();
            double gap = probe.gap(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
            if (gap <= bestGap)
            {
                bestGap = gap;
                best = candidate;
            }
        }
        return best;
    }

    private record Space(ServerPlayer player, ServerLevel level, @Nullable SolarSystemDefinition system)
    {
    }
}
