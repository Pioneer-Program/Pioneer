package cute.ame.pioneer.Computer.Logic;

import net.minecraft.resources.ResourceLocation;

public abstract class Condition extends Axiom implements Conditional {

    public Condition(ResourceLocation type) {
        super(type);
    }
}
