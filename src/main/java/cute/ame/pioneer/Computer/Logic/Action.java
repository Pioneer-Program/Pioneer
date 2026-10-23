package cute.ame.pioneer.Computer.Logic;

import net.minecraft.resources.ResourceLocation;

public abstract class Action extends Axiom implements Executable {

    public Action(ResourceLocation type) {
        super(type);
    }
}
