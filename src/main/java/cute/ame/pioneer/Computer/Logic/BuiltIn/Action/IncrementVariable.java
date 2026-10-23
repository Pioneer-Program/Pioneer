package cute.ame.pioneer.Computer.Logic.BuiltIn.Action;

import com.google.gson.annotations.SerializedName;
import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Computer.Logic.Variable;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class IncrementVariable extends Action {

    public final static ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "increment_variable");

    private transient Variable<?> variable;

    @SerializedName("variable")
    private String variableName;

    private Object value;

    public IncrementVariable(String variableName, Object value) {
        super(TYPE);
        this.variable = null;
        this.variableName = variableName;
        this.value = value;
    }

    @Override
    public boolean execute(HashMap<String, Variable<?>> variables) {
        if (variable == null)
            resolve(variables);
        Object variableValue = variable.getValue();
        if (variableValue instanceof Number a && value instanceof Number b) {
            Number result = a.doubleValue() + b.doubleValue();
            variable.assign(result);
        }
        return true;
    }

    public Variable<?> getVariable() {
        return variable;
    }

    public void setVariable(Variable<?> variable) {
        this.variable = variable;
    }

    public void resolve(HashMap<String, Variable<?>> variable) {
        this.variable = variable.get(variableName);
    }
}
