package cute.ame.pioneer.Computer.Logic.BuiltIns;

import cute.ame.pioneer.Computer.Logic.Variable;
import cute.ame.pioneer.Pioneer;

public class VariableCondition extends Condition {

    private Variable<?> variable;

    public VariableCondition() {
        super(Pioneer.MODID, "variable");
    }

    @Override
    public boolean isSatisfied() {
        return false;
    }
}
