package cute.ame.pioneer.Computer.Logic.BuiltIn.Action;

import com.google.gson.annotations.SerializedName;
import cute.ame.pioneer.Computer.BlockEntity.MagneticCoreMemoryEntity;
import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Computer.Logic.Variable;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public abstract class VariableAction extends Action {

    protected transient Variable<?> variable;

    @SerializedName("variable")
    protected String variableName;

    public VariableAction(ResourceLocation type, String variableName) {
        super(type);
        this.variableName = variableName;
        this.variable = null;
    }

    public abstract boolean evaluate(MagneticCoreMemoryEntity memory);

    @Override
    public boolean execute(MagneticCoreMemoryEntity memory) {
        if (variable == null) {
            if (!resolve(memory))
                throw new RuntimeException("Variable '" + variableName + "' is undefined");
        }
        return evaluate(memory);
    }

    public boolean resolve(MagneticCoreMemoryEntity memory) {
        variable = memory.getVariables().get(variableName);
        return variable != null;
    }
}
