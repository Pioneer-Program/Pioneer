package cute.ame.pioneer.Computer.Logic.BuiltIn.Action;

import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

public class ChangeVariable extends Action {

    public final static ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "change_variable");

    public ChangeVariable() {
        super(TYPE);
    }

    @Override
    public boolean execute() {
        System.out.println("AAA");
        return false;
    }
}
