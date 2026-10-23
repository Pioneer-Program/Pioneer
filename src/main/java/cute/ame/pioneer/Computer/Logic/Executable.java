package cute.ame.pioneer.Computer.Logic;

import java.util.HashMap;

public interface Executable {

    boolean execute(HashMap<String, Variable<?>> variables);

}
