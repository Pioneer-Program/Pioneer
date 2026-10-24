package cute.ame.pioneer.Computer.Logic;

import cute.ame.pioneer.Computer.BlockEntity.MagneticCoreMemoryEntity;

import java.util.HashMap;

public class UserFunction extends Function {

    private String name;
    private String description;
    private Function function;


    public UserFunction(String name, String description, Function function) {
        super(function.getType(), function.getAction(), function.getCondition());
        this.name = name;
        this.description = description;
        this.function = function;
        this.action = function.getAction();
        this.condition = function.getCondition();
    }

    @Override
    public boolean execute(MagneticCoreMemoryEntity memory) {
        return function.execute(memory);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Function getFunction() {
        return function;
    }

}
