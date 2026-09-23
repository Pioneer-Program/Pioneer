package cute.ame.pioneer.Command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class PioneerDebugCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("pdb") // the joke here is that pdb files are used for debugging, and this command too (also stand for /PioneerDebug)
            .requires(src -> src.hasPermission(2))
            .then(ShadingDebugCommand.build())
            .then(ObserverDebugCommand.build())
            .then(TimeDebugCommand.build())
        );
    }
}
