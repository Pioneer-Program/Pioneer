package cute.ame.pioneer.Command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cute.ame.pioneer.Config;
import cute.ame.pioneer.LifeSupport.Level.KelpBeds;
import cute.ame.pioneer.LifeSupport.Physics.Photosynthesis;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

import static cute.ame.pioneer.Command.PioneerCommandFeedback.PREFIX;

public final class KelpDebugCommand
{
    public static ArgumentBuilder<CommandSourceStack, ?> build()
    {
        return Commands.literal("kelp").executes(KelpDebugCommand::list);
    }

    private static int list(CommandContext<CommandSourceStack> ctx)
    {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        KelpBeds beds = KelpBeds.getIfPresent(level);
        if (beds == null)
        {
            source.sendSuccess(() -> Component.literal(PREFIX + ChatFormatting.WHITE + "kelp beds " + ChatFormatting.GOLD + "0 " + ChatFormatting.DARK_GRAY + "(no vent tracked in " + level.dimension().location() + ")"), false);
            return 0;
        }

        List<KelpBeds.Bed> list = beds.beds();
        int productive = 0;
        for (KelpBeds.Bed bed : list) if (bed.plants() > 0) productive++;

        final int total = list.size(), working = productive, pending = beds.pendingCount();
        source.sendSuccess(() -> Component.literal(PREFIX + String.format(ChatFormatting.WHITE + "kelp beds " + ChatFormatting.GOLD + "%d " + ChatFormatting.GRAY + "| with kelp " + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "| pending vents " + ChatFormatting.YELLOW + "%d", total, working, pending)), false);
        float rate = Config.KELP_MOL_PER_TICK.get().floatValue();
        for (KelpBeds.Bed bed : list)
        {
            float light = beds.light(level, bed);
            float perTick = Photosynthesis.capacity(bed.plants(), light, rate, 1);
            String vent = bed.vents() == 0 ? "-" : bed.vent(0).toShortString();
            source.sendSuccess(() -> Component.literal(String.format("  " + ChatFormatting.GRAY + "vent " + ChatFormatting.DARK_GRAY + "%s " + ChatFormatting.GRAY + "(x%d) | kelp " + ChatFormatting.GREEN + "%d " + ChatFormatting.GRAY + "| cells " + ChatFormatting.AQUA + "%d " + ChatFormatting.GRAY + "| light " + ChatFormatting.YELLOW + "%.2f " + ChatFormatting.GRAY + "| max " + ChatFormatting.AQUA + "%.6f mol/t", vent, bed.vents(), bed.plants(), bed.cells(), light, perTick)), false);
        }

        return total;
    }
}
