package cute.ame.pioneer.Command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.Core.Thermal.BlockTemperature;
import cute.ame.pioneer.Thermal.Data.MaterialTable;
import cute.ame.pioneer.Thermal.Data.ThermalMaterial;
import cute.ame.pioneer.Thermal.Data.ThermalStore;
import cute.ame.pioneer.Thermal.Event.ThermalTickEvents;
import cute.ame.pioneer.Thermal.Level.ThermalLevelData;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterialRegistry;
import cute.ame.pioneer.Thermal.Registry.ThermalMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.ERROR_PREFIX;
import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class ThermalDebugCommand
{
    private static final int LIST_LIMIT = 16; // TRUST ME, THIS IS ENOUGH

    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("thermal")
            .then(Commands.literal("at")
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ThermalDebugCommand::at)))
            .then(Commands.literal("set")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("kelvin", FloatArgumentType.floatArg(0.0f)).executes(ThermalDebugCommand::set))))
            .then(Commands.literal("clear")
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ThermalDebugCommand::clear)))
            .then(Commands.literal("purge").executes(ThermalDebugCommand::purge))
            .then(Commands.literal("list").executes(ThermalDebugCommand::list))
            .then(Commands.literal("materials").executes(ThermalDebugCommand::materials))
            .then(Commands.literal("step").executes(ctx -> step(ctx, 1))
                .then(Commands.argument("passes", IntegerArgumentType.integer(1, 1000)).executes(ctx -> step(ctx, IntegerArgumentType.getInteger(ctx, "passes")))))

            ;
    }

    private static int at(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");

        BlockState state = level.getBlockState(pos);
        int material = ThermalMaterials.indexOf(state);
        MaterialTable table = ThermalMaterials.table();

        ThermalLevelData data = ThermalLevelData.getIfPresent(level);
        float stored = data == null ? Float.NaN : data.kelvinAt(pos);
        boolean tracked = !Float.isNaN(stored);
        float effective = BlockTemperature.of(level, pos);
        float breakdown = table.breakdownK(material);

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%s " + ChatFormatting.DARK_GRAY + "%s " + ChatFormatting.GRAY + "| " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%s)", pos.toShortString(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), effective, tracked ? "tracked" : "ambient")), false);
        source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.YELLOW + "%-24s " + ChatFormatting.GRAY + "k = " + ChatFormatting.AQUA + "%.3f W/m/K " + ChatFormatting.GRAY + "C = " + ChatFormatting.AQUA + "%.3e J/K " + ChatFormatting.GRAY + "eps = " + ChatFormatting.AQUA + "%.2f " + ChatFormatting.GRAY + "breaks at " + ChatFormatting.RED + "%s", table.key(material), table.conductivity(material), table.volumetricHeat(material), table.emissivity(material), breakdown >= ThermalMaterial.NEVER_BREAKS ? "never" : String.format("%.0f K", breakdown))), false);
        return tracked ? 1 : 0;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        float kelvin = FloatArgumentType.getFloat(ctx, "kelvin");
        if (level.getBlockState(pos).isAir())
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + "nothing to heat at " + pos.toShortString()));
            return 0;
        }

        ThermalLevelData data = ThermalLevelData.get(level);
        if (!data.force(pos, kelvin))
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + "store is full (" + data.count() + " entries)"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%s " + ChatFormatting.GRAY + "-> " + ChatFormatting.GREEN + "%.2f K " + ChatFormatting.DARK_GRAY + "(%d tracked)", pos.toShortString(), kelvin, data.count())), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ThermalLevelData data = ThermalLevelData.getIfPresent(source.getLevel());
        if (data == null || !data.detach(pos))
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + pos.toShortString() + " was not tracked"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + pos.toShortString() + ChatFormatting.GRAY + " released to ambient"), true);
        return 1;
    }

    private static int purge(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ThermalLevelData data = ThermalLevelData.getIfPresent(source.getLevel());
        int removed = data == null ? 0 : data.purge();

        source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + removed + ChatFormatting.GRAY + " entrie(s) dropped"), true);
        return removed;
    }

    private static int list(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        ThermalLevelData data = ThermalLevelData.getIfPresent(level);
        if (data == null || data.count() == 0)
        {
            source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.GRAY + "nothing tracked in " + ChatFormatting.DARK_GRAY + level.dimension().location()), false);
            return 0;
        }

        ThermalStore store = data.store();
        int count = store.count();
        float ambient = BlockTemperature.dimensionDefault(level);
        MaterialTable table = ThermalMaterials.table();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d " + ChatFormatting.GRAY + "tracked | capacity = " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "| cursor = " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "| pass = " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "| ambient = " + ChatFormatting.GREEN + "%.2f K", count, store.capacity(), store.cursor(), store.pass(), ambient)), false);
        int[] top = new int[LIST_LIMIT];
        float[] scores = new float[LIST_LIMIT];
        int shown = 0;

        for (int slot = 0; slot < count; slot++)
        {
            float score = Math.abs(store.kelvin(slot) - ambient);
            if (shown == LIST_LIMIT && score <= scores[LIST_LIMIT - 1]) continue;

            int at = Math.min(shown, LIST_LIMIT - 1);
            while (at > 0 && scores[at - 1] < score)
            {
                top[at] = top[at - 1];
                scores[at] = scores[at - 1];
                at--;
            }

            top[at] = slot;
            scores[at] = score;
            if (shown < LIST_LIMIT) shown++;
        }

        for (int i = 0; i < shown; i++)
        {
            int slot = top[i];
            long packed = store.position(slot);
            int material = store.material(slot);

            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.GOLD + "%-8s " + ChatFormatting.GREEN + "%8.2f K " + ChatFormatting.GRAY + "%+7.2f " + ChatFormatting.DARK_GRAY + "%s", BlockPos.of(packed).toShortString(), store.kelvin(slot), store.kelvin(slot) - ambient, material == ThermalStore.UNRESOLVED ? "unresolved" : table.key(material))), false);
        }

        if (count > shown)
        {
            final int rest = count - shown;
            source.sendSuccess(() -> Component.literal("  " + ChatFormatting.DARK_GRAY + "... and " + rest + " more"), false);
        }

        return count;
    }

    private static int materials(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        MaterialTable table = ThermalMaterials.table();

        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d " + ChatFormatting.GRAY + "material(s) from " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "datapack file(s), covering " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "block(s)", table.size(), ThermalMaterialRegistry.loadedCount(), ThermalMaterials.coveredBlocks())), false);

        for (int i = 0; i < table.size(); i++)
        {
            final int material = i;
            float breakdown = table.breakdownK(material);

            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.DARK_GRAY + "%2d " + ChatFormatting.YELLOW + "%-8s " + ChatFormatting.GRAY + "k = " + ChatFormatting.AQUA + "%8.3f " + ChatFormatting.GRAY + "C = " + ChatFormatting.AQUA + "%.3e " + ChatFormatting.GRAY + "eps = " + ChatFormatting.AQUA + "%.2f " + ChatFormatting.GRAY + "-> " + ChatFormatting.RED + "%s", material, table.key(material), table.conductivity(material), table.volumetricHeat(material), table.emissivity(material), breakdown >= ThermalMaterial.NEVER_BREAKS ? "never" : String.format("%.0f K", breakdown))), false);
        }

        return table.size();
    }

    private static int step(CommandContext<CommandSourceStack> ctx, int passes)
    {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        ThermalLevelData data = ThermalLevelData.getIfPresent(level);
        if (data == null || data.count() == 0)
        {
            source.sendFailure(Component.literal(ERROR_PREFIX + "nothing tracked to step"));
            return 0;
        }

        int period = Math.max(Config.THERMAL_PERIOD.get(), 1);
        int visited = 0;
        for (int i = 0; i < passes; i++) visited += ThermalTickEvents.pass(level, data, period);

        final int total = visited;
        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "%d " + ChatFormatting.GRAY + "pass(es), " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "visit(s), " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "tracked", passes, total, data.count())), false);
        return total;
    }
}
