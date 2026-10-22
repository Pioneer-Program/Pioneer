package cute.ame.pioneer.Computer.Logic.BuiltIns;

import cute.ame.pioneer.Computer.Logic.Axiom;
import cute.ame.pioneer.Computer.Logic.Conditional;

public abstract class Condition extends Axiom implements Conditional {

    public Condition(String type) {
        super(type);
    }

    public Condition(String modId, String name) {
        super(modId, name);
    }
}
