package cute.ame.pioneer.Computer.Logic.BuiltIn.Action;

import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Computer.Logic.Variable;
import cute.ame.pioneer.Pioneer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;

public class ChangeVariable extends Action {

    public final static ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "change_variable");

    public ChangeVariable() {
        super(TYPE);
    }

    @Override
    public boolean execute(HashMap<String, Variable<?>> variables) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        server.getPlayerList().broadcastSystemMessage(Component.literal("Test"), false);
        return true;
    }
}
