package cute.ame.pioneer.Computer.Logic;

import net.minecraft.resources.ResourceLocation;

public abstract class Axiom {

    protected ResourceLocation type;

    public Axiom(ResourceLocation type) {
        this.type = type;
    }

    public ResourceLocation getType() {
        return type;
    }

    public void setType(ResourceLocation type) {
        this.type = type;
    }

}
