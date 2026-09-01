package cute.ame.pioneer.Command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Fluid.Helper.AmbientResolver;
import cute.ame.pioneer.Fluid.Data.AmbientState;
import cute.ame.pioneer.Fluid.Data.FluidConstants;
import cute.ame.pioneer.Fluid.Data.FluidNodeStore;
import cute.ame.pioneer.Fluid.Registry.FluidSpecies;
import cute.ame.pioneer.Fluid.Level.FluidLevelData;
import cute.ame.pioneer.Fluid.Physics.ComponentPartition;
import cute.ame.pioneer.Fluid.Graph.FluidGraph;
import cute.ame.pioneer.Fluid.Physics.FluidHeat;
import cute.ame.pioneer.Fluid.Level.RoomLevelData;
import cute.ame.pioneer.Fluid.BlockEntity.FluidVesselBlockEntity;
import cute.ame.pioneer.Fluid.Data.SpeciesTable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.ERROR_PREFIX;
import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class FluidDebugCommand
{
    private static final SuggestionProvider<CommandSourceStack> SPECIES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(FluidSpecies.active().keys(), builder);

    private static final int LIST_LIMIT = 32;

    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("fluid")
            .then(Commands.literal("create")
                .then(Commands.argument("volume", FloatArgumentType.floatArg(0.001f)).executes(ctx -> create(ctx, FluidConstants.DEFAULT_TEMPERATURE_K))
                    .then(Commands.argument("kelvin", FloatArgumentType.floatArg(0.1f)).executes(ctx -> create(ctx, FloatArgumentType.getFloat(ctx, "kelvin"))))))
            .then(Commands.literal("tank").executes(ctx -> create(ctx, FluidConstants.TANK_VOLUME_L, FluidConstants.DEFAULT_TEMPERATURE_K)))
            .then(Commands.literal("pipe").executes(ctx -> create(ctx, FluidConstants.PIPE_VOLUME_L, FluidConstants.DEFAULT_TEMPERATURE_K)))
            .then(Commands.literal("destroy")
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(FluidDebugCommand::destroy)))
            .then(Commands.literal("inject")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                    .then(Commands.argument("species", StringArgumentType.word()).suggests(SPECIES)
                        .then(Commands.argument("mol", FloatArgumentType.floatArg()).executes(FluidDebugCommand::inject)))))
            .then(Commands.literal("phase")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                    .then(Commands.literal("liquid").executes(ctx -> phase(ctx, true)))
                    .then(Commands.literal("gas").executes(ctx -> phase(ctx, false)))))
            .then(Commands.literal("heat")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                    .then(Commands.argument("joules", DoubleArgumentType.doubleArg()).executes(FluidDebugCommand::heat))))
            .then(Commands.literal("temp")
                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                    .then(Commands.argument("kelvin", FloatArgumentType.floatArg(0.1f)).executes(FluidDebugCommand::temp))))
            .then(Commands.literal("info")
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(FluidDebugCommand::info)))
            .then(Commands.literal("list").executes(FluidDebugCommand::list))
            .then(Commands.literal("species").executes(FluidDebugCommand::species))
            .then(Commands.literal("net").executes(FluidDebugCommand::net)
                .then(Commands.literal("wake").executes(FluidDebugCommand::netWake)))
            .then(Commands.literal("at")
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(FluidDebugCommand::at)))
            .then(Commands.literal("room").executes(FluidDebugCommand::room)
                .then(Commands.literal("remove").executes(FluidDebugCommand::roomRemove))
                .then(Commands.literal("list").executes(FluidDebugCommand::roomList)))
            .then(Commands.literal("ambient").executes(FluidDebugCommand::ambient))

            ;
    }

    private static int create(CommandContext<CommandSourceStack> ctx, float kelvin)
    {
        return create(ctx, FloatArgumentType.getFloat(ctx, "volume"), kelvin);
    }

    private static int create(CommandContext<CommandSourceStack> ctx, float litres, float kelvin)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);

        int id = data.store().create(litres, kelvin);
        data.setDirty();

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "node " + ChatFormatting.GOLD + "#%d " + ChatFormatting.GRAY + "created | " + ChatFormatting.AQUA + "%.1f L " + ChatFormatting.GRAY + "| " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%s)", id, litres, kelvin, level.dimension().location())), false);
        return id + 1;
    }

    private static int destroy(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);
        int id = IntegerArgumentType.getInteger(ctx, "id");

        RoomLevelData rooms = RoomLevelData.getIfPresent(level);
        if (rooms != null && rooms.room(id) != null)
        {
            rooms.remove(level, id);
            ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "room " + ChatFormatting.GOLD + "#" + id + ChatFormatting.GRAY + " detached and destroyed"), false);
            return 1;
        }

        if (!data.store().destroy(id)) return missing(ctx, id);

        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "node " + ChatFormatting.GOLD + "#" + id + ChatFormatting.GRAY + " destroyed"), false);
        return 1;
    }

    private static int inject(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);
        FluidNodeStore store = data.store();
        SpeciesTable table = FluidSpecies.active();

        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!store.alive(id)) return missing(ctx, id);

        String key = StringArgumentType.getString(ctx, "species");
        int species = table.indexOf(key);
        if (!table.isValid(species))
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "unknown species: " + ChatFormatting.YELLOW + key + ChatFormatting.GRAY + " (" + String.join(", ", table.keys()) + ")"));
            return 0;
        }

        float mol = FloatArgumentType.getFloat(ctx, "mol");
        store.add(id, species, mol);
        data.touch(id);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "#%d " + ChatFormatting.GRAY + "%+.3f mol " + ChatFormatting.YELLOW + "%s " + ChatFormatting.GRAY + "| n = " + ChatFormatting.AQUA + "%.3f mol " + ChatFormatting.GRAY + "| P = " + ChatFormatting.GREEN + "%.5f P", id, mol, key, store.moles(id), store.pressure(id))), false);
        return 1;
    }

    private static int temp(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);
        FluidNodeStore store = data.store();

        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!store.alive(id)) return missing(ctx, id);

        float kelvin = FloatArgumentType.getFloat(ctx, "kelvin");
        store.setTemperature(id, kelvin);
        data.touch(id);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "#%d " + ChatFormatting.GRAY + "T = " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%.2f °C) " + ChatFormatting.GRAY + "| P = " + ChatFormatting.GREEN + "%.5f P", id, kelvin, FluidConstants.toCelsius(kelvin), store.pressure(id))), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidNodeStore store = FluidLevelData.get(level).store();
        SpeciesTable table = FluidSpecies.active();

        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!store.alive(id)) return missing(ctx, id);

        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "node " + ChatFormatting.GOLD + "#%d " + ChatFormatting.GRAY + "| V = " + ChatFormatting.AQUA + "%.1f L " + ChatFormatting.GRAY + "| T = " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%.2f °C) " + ChatFormatting.GRAY + "| n = " + ChatFormatting.AQUA + "%.4f mol " + ChatFormatting.GRAY + "| P = " + ChatFormatting.GREEN + "%.5f P " + ChatFormatting.GRAY + "| %s", id, store.volume(id), store.temperature(id), FluidConstants.toCelsius(store.temperature(id)), store.moles(id), store.pressure(id), (store.isLiquid(id) ? "liquid" : "gas") + (store.hasFlag(id, FluidNodeStore.FLAG_OPEN) ? ", open" : ", sealed") + (store.latent(id) > 0.0f ? String.format(", boiling %.0f J", store.latent(id)) : ""))), false);

        if (store.moles(id) <= 0.0f)
        {
            source.sendSuccess(() -> Component.literal("  " + ChatFormatting.DARK_GRAY + "empty"), false);
            return 1;
        }

        for (int s = 0; s < store.getStride(); s++)
        {
            float mol = store.amount(id, s);
            if (mol <= 0.0f) continue;

            final int species = s;
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.YELLOW + "%-4s " + ChatFormatting.GRAY + "%.4f mol " + ChatFormatting.DARK_GRAY + "(%.2f %%)", table.key(species), mol, store.fraction(id, species) * 100.0f)), false);
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidNodeStore store = FluidLevelData.get(level).store();
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d " + ChatFormatting.GRAY + "live node(s) | slots = " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "| capacity = " + ChatFormatting.AQUA + "%d " + ChatFormatting.DARK_GRAY + "(%s)", store.getLiveCount(), store.getHighWater(), store.getCapacity(), level.dimension().location())), false);

        int shown = 0;
        for (int id = 0; id < store.getHighWater() && shown < LIST_LIMIT; id++)
        {
            if (!store.alive(id)) continue;
            shown++;

            final int node = id;
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.GOLD + "#%-4d " + ChatFormatting.AQUA + "%8.1f L " + ChatFormatting.GREEN + "%7.2f K " + ChatFormatting.GRAY + "%9.4f mol " + ChatFormatting.GREEN + "%9.5f P", node, store.volume(node), store.temperature(node), store.moles(node), store.pressure(node))), false);
        }

        if (store.getLiveCount() > shown)
        {
            final int rest = store.getLiveCount() - shown;
            source.sendSuccess(() -> Component.literal("  " + ChatFormatting.DARK_GRAY + "... and " + rest + " more"), false);
        }
        return store.getLiveCount();
    }

    private static int species(CommandContext<CommandSourceStack> ctx)
    {
        SpeciesTable table = FluidSpecies.active();
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + table.size()
                + ChatFormatting.GRAY + " species, world order:"), false);

        for (int s = 0; s < table.size(); s++)
        {
            final int species = s;
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.DARK_GRAY + "%2d " + ChatFormatting.YELLOW + "%-5s " + ChatFormatting.GRAY + "M = " + ChatFormatting.AQUA + "%7.3f g/mol " + ChatFormatting.GRAY + "c = " + ChatFormatting.AQUA + "%7.1f J/kg/K", species, table.key(species), table.molarMass(species), table.specificHeat(species))), false);
        }
        return table.size();
    }

    private static int room(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();

        RoomLevelData rooms = RoomLevelData.get(level);
        int nodeId = rooms.attach(level, pos);

        if (nodeId == FluidNodeStore.INVALID)
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "position is not passable - stand inside an air block"));
            return 0;
        }

        reportRoom(ctx.getSource(), level, rooms, nodeId);
        return nodeId + 1;
    }

    private static int roomRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();

        RoomLevelData rooms = RoomLevelData.get(level);
        int nodeId = rooms.nodeAt(player.blockPosition());

        if (nodeId == FluidNodeStore.INVALID)
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "no room at this position"));
            return 0;
        }

        rooms.remove(level, nodeId);
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "room " + ChatFormatting.GOLD + "#" + nodeId + ChatFormatting.GRAY + " detached"), false);
        return 1;
    }

    private static int roomList(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        RoomLevelData rooms = RoomLevelData.get(level);
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d " + ChatFormatting.GRAY + "room(s) | pending rescans = " + ChatFormatting.AQUA + "%d " + ChatFormatting.DARK_GRAY + "(%s)", rooms.roomCount(), rooms.pendingRescans(), level.dimension().location())), false);

        for (RoomLevelData.Room room : rooms.rooms()) reportRoom(source, level, rooms, room.nodeId());
        return rooms.roomCount();
    }

    private static void reportRoom(CommandSourceStack source, ServerLevel level, RoomLevelData rooms, int nodeId)
    {
        RoomLevelData.Room room = rooms.room(nodeId);
        if (room == null) return;

        FluidNodeStore store = FluidLevelData.get(level).store();
        boolean alive = store.alive(nodeId);
        int cells = room.cells().length;

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "room " + ChatFormatting.GOLD + "#%d " + ChatFormatting.GRAY + "| " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "block(s) | V = " + ChatFormatting.AQUA + "%.0f L " + ChatFormatting.GRAY + "| " + (room.sealed() ? ChatFormatting.GREEN + "sealed" : ChatFormatting.RED + "open") + ChatFormatting.GRAY + " | P = " + ChatFormatting.GREEN + "%.5f P%s", nodeId, cells, alive ? store.volume(nodeId) : 0.0f, alive ? store.pressure(nodeId) : 0.0, cells >= Config.ROOM_MAX_BLOCKS.get() ? ChatFormatting.YELLOW + " [cap reached]" : "")), false);
    }

    private static int at(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");

        if (!(level.getBlockEntity(pos) instanceof FluidVesselBlockEntity vessel))
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "no fluid vessel at " + pos.toShortString()));
            return 0;
        }

        FluidNodeStore store = FluidLevelData.get(level).store();
        int nodeId = store.resolve(vessel.getNodeHandle());

        if (nodeId == FluidNodeStore.INVALID)
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "vessel at " + pos.toShortString() + " holds a stale handle"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "vessel " + ChatFormatting.DARK_GRAY + "%s " + ChatFormatting.GRAY + "-> node " + ChatFormatting.GOLD + "#%d " + ChatFormatting.GRAY + "| V = " + ChatFormatting.AQUA + "%.1f L " + ChatFormatting.GRAY + "| n = " + ChatFormatting.AQUA + "%.4f mol " + ChatFormatting.GRAY + "| P = " + ChatFormatting.GREEN + "%.5f P " + ChatFormatting.GRAY + "| bursts at " + ChatFormatting.RED + "%.2f P", pos.toShortString(), nodeId, store.volume(nodeId), store.moles(nodeId), store.pressure(nodeId), vessel.getBurstPressure())), false);
        return nodeId + 1;
    }

    private static int net(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);
        FluidGraph graph = data.graph();
        FluidNodeStore store = data.store();

        graph.rebuildIfDirty(level, store);

        ComponentPartition.Result partition = graph.partition();
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d " + ChatFormatting.GRAY + "network(s) | " + ChatFormatting.GREEN + "%d awake " + ChatFormatting.GRAY + "| vessels = " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "| edges = " + ChatFormatting.AQUA + "%d " + ChatFormatting.DARK_GRAY + "(%s)", partition.count(), graph.awakeCount(), graph.vesselCount(), graph.edgeCount(), level.dimension().location())), false);

        for (int c = 0; c < partition.count() && c < LIST_LIMIT; c++)
        {
            double volume = 0.0;
            double moles = 0.0;
            double lowest = Double.MAX_VALUE;
            double highest = 0.0;
            double coldest = Double.MAX_VALUE;
            double hottest = 0.0;

            for (int i = partition.nodeOffsets()[c]; i < partition.nodeOffsets()[c + 1]; i++)
            {
                int nodeId = partition.nodeOrder()[i];
                if (!store.alive(nodeId)) continue;

                volume += store.volume(nodeId);
                moles += store.moles(nodeId);

                double pressure = store.pressure(nodeId);
                if (pressure < lowest) lowest = pressure;
                if (pressure > highest) highest = pressure;

                float temperature = store.temperature(nodeId);
                if (temperature < coldest) coldest = temperature;
                if (temperature > hottest) hottest = temperature;
            }

            final int component = c;
            final double totalVolume = volume;
            final double totalMoles = moles;
            final double minPressure = lowest == Double.MAX_VALUE ? 0.0 : lowest;
            final double maxPressure = highest;
            final double minTemperature = coldest == Double.MAX_VALUE ? 0.0 : coldest;
            final double maxTemperature = hottest;

            final String state = graph.isAsleep(component) ? ChatFormatting.DARK_GRAY + "asleep" : ChatFormatting.GREEN + "awake";

            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.GOLD + "net %-3d " + ChatFormatting.GRAY + "nodes = " + ChatFormatting.AQUA + "%-4d " + ChatFormatting.GRAY + "edges = " + ChatFormatting.AQUA + "%-4d " + ChatFormatting.GRAY + "V = " + ChatFormatting.AQUA + "%9.1f L " + ChatFormatting.GRAY + "n = " + ChatFormatting.AQUA + "%9.3f mol " + ChatFormatting.GRAY + "P = " + ChatFormatting.GREEN + "%.5f" + ChatFormatting.GRAY + " .. " + ChatFormatting.GREEN + "%.5f P " + ChatFormatting.GRAY + "T = " + ChatFormatting.GREEN + "%.1f" + ChatFormatting.GRAY + " .. " + ChatFormatting.GREEN + "%.1f K " + ChatFormatting.GRAY + "| %s", component, partition.nodeCount(component), partition.edgeCount(component), totalVolume, totalMoles, minPressure, maxPressure, minTemperature, maxTemperature, state)), false);
        }

        return partition.count();
    }

    private static int netWake(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);

        data.graph().wakeAll();
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GRAY + "every network woken"), false);
        return data.graph().awakeCount();
    }

    private static int heat(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);
        FluidNodeStore store = data.store();

        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!store.alive(id)) return missing(ctx, id);

        double joules = DoubleArgumentType.getDouble(ctx, "joules");
        float[] molarHeat = FluidSpecies.active().molarHeatRaw();

        double capacity = FluidHeat.capacity(store, id, molarHeat);
        if (capacity <= 0.0)
        {
            ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "node #" + id + " is empty - nothing to heat"));
            return 0;
        }

        float delta = FluidHeat.addJoules(store, id, joules, molarHeat);
        data.touch(id);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "#%d " + ChatFormatting.GRAY + "%+.1f J " + ChatFormatting.GRAY + "| C = " + ChatFormatting.AQUA + "%.2f J/K " + ChatFormatting.GRAY + "| " + ChatFormatting.GREEN + "%+.2f K " + ChatFormatting.GRAY + "-> " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%.2f °C)", id, joules, capacity, delta, store.temperature(id), FluidConstants.toCelsius(store.temperature(id)))), false);
        return 1;
    }

    private static int phase(CommandContext<CommandSourceStack> ctx, boolean liquid)
    {
        ServerLevel level = ctx.getSource().getLevel();
        FluidLevelData data = FluidLevelData.get(level);
        FluidNodeStore store = data.store();

        int id = IntegerArgumentType.getInteger(ctx, "id");
        if (!store.alive(id)) return missing(ctx, id);

        store.setFlag(id, FluidNodeStore.FLAG_LIQUID, liquid);
        store.setLatent(id, 0.0f);
        data.touch(id);

        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "#" + id + ChatFormatting.GRAY + " forced to " + ChatFormatting.YELLOW + (liquid ? "liquid" : "gas")), false);
        return 1;
    }

    private static int missing(CommandContext<CommandSourceStack> ctx, int id)
    {
        ctx.getSource().sendFailure(Component.literal(ERROR_PREFIX + "no live node at id " + ChatFormatting.YELLOW + "#" + id));
        return 0;
    }

    private static int ambient(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        AmbientState ambient = AmbientResolver.of(level.dimension());
        SpeciesTable table = FluidSpecies.active();
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "ambient " + ChatFormatting.DARK_GRAY + "(%s) " + ChatFormatting.GRAY + "| P = " + ChatFormatting.GREEN + "%.5f P " + ChatFormatting.GRAY + "| T = " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%.2f °C) " + ChatFormatting.GRAY + "| %s", level.dimension().location(), ambient.pressureP(), ambient.temperatureK(), FluidConstants.toCelsius(ambient.temperatureK()), ambient.vacuum() ? ChatFormatting.RED + "vacuum" : ChatFormatting.GREEN + "atmosphere")), false);

        if (ambient.vacuum()) return 1;

        for (int s = 0; s < table.size(); s++)
        {
            float fraction = ambient.fraction(s);
            if (fraction <= 0.0f) continue;

            final int species = s;
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.YELLOW + "%-4s " + ChatFormatting.GRAY + "%.2f %%", table.key(species), ambient.fraction(species) * 100.0f)), false);
        }
        return 1;
    }

}