package cute.ame.pioneer.Computer.Logic;

import cute.ame.pioneer.Computer.BlockEntity.MagneticCoreMemoryEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class Function extends Axiom implements Executable  {

    protected Action action;
    protected Condition condition;

    public Function(ResourceLocation type, Action action, Condition condition) {
        super(type);
        this.action = action;
        this.condition = condition;
    }

    @Override
    public boolean execute(MagneticCoreMemoryEntity memory) {
        if (action == null)
            return false;
        if (condition == null || condition.isSatisfied())
            action.execute(memory);
        return false;
    }

    public Action getAction() {
        return action;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void  setCondition(Condition condition) {
        this.condition = condition;
    }
}

