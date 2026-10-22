package cute.ame.pioneer.Computer.Logic;

import cute.ame.pioneer.Pioneer;

public abstract class Action extends Axiom implements Executable {

    public Action(String type) {
        super(type);
    }

    public Action(String modId, String name) {
        super(modId, name);
    }
}
