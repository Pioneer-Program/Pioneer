package cute.ame.pioneer.Computer.Logic;

import net.minecraft.resources.ResourceLocation;

public class Function extends Axiom implements Executable  {

    protected Action action;
    protected Condition condition;

    public Function(ResourceLocation type, Action action, Condition condition) {
        super(type);
        this.action = action;
        this.condition = condition;
    }

    @Override
    public boolean execute() {
        if (condition == null || condition.isSatisfied())
            action.execute();
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

