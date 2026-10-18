package cute.ame.pioneer.Item.Tune;

import cute.ame.pioneer.Core.Configure.Tunable;
import cute.ame.pioneer.Core.Configure.TuneOption;
import cute.ame.pioneer.Core.Configure.TuneOptions;
import cute.ame.pioneer.Item.Network.TuneOptionsPayload;
import cute.ame.pioneer.Item.Network.TuneSetPayload;
import cute.ame.pioneer.Item.ToolBoxItem;
import cute.ame.pioneer.Registrie.ModToolBoxModules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TuneServer
{
    public static void request(ServerPlayer player, long packed)
    {
        BlockPos pos = BlockPos.of(packed);
        Tunable tunable = target(player, pos);
        PacketDistributor.sendToPlayer(player, new TuneOptionsPayload(packed, tunable == null ? List.of() : collect(tunable, player.serverLevel(), pos)));
    }

    public static void set(ServerPlayer player, long packed, int option, double value)
    {
        BlockPos pos = BlockPos.of(packed);
        Tunable tunable = target(player, pos);
        if (tunable == null) return;

        ServerLevel level = player.serverLevel();
        List<TuneOption> options = collect(tunable, level, pos);
        int index = option;
        double next = value;
        if (option == TuneSetPayload.TOGGLE)
        {
            index = firstToggle(options);
            if (index < 0) return;

            next = options.get(index).value() >= 0.5 ? 0.0 : 1.0;
        }

        if (index < 0 || index >= options.size() || !Double.isFinite(next)) return;

        TuneOption chosen = options.get(index);
        next = chosen.sanitize(next);
        BlockState state = level.getBlockState(pos);
        if (!tunable.tune(level, pos, state, index, next)) return;
        if (option == TuneSetPayload.TOGGLE)
            player.displayClientMessage(Component.translatable("readout.pioneer.line", Component.translatable(chosen.key()), chosen.label(next)), true);

        PacketDistributor.sendToPlayer(player, new TuneOptionsPayload(packed, collect(tunable, level, pos)));
    }

    public static int firstToggle(List<TuneOption> options)
    {
        for (int i = 0; i < options.size(); i++) if (options.get(i).isToggle()) return i;

        return -1;
    }

    private static List<TuneOption> collect(Tunable tunable, ServerLevel level, BlockPos pos)
    {
        TuneOptions out = new TuneOptions();
        tunable.options(level, pos, level.getBlockState(pos), out);

        return out.list();
    }

    private static @Nullable Tunable target(ServerPlayer player, BlockPos pos)
    {
        ItemStack stack = ToolBoxItem.held(player);
        if (stack.isEmpty() || ToolBoxItem.moduleOf(stack) != ModToolBoxModules.CONFIGURE.get()) return null;

        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, 1.0)) return null;

        return level.getBlockState(pos).getBlock() instanceof Tunable tunable ? tunable : null;
    }
}
