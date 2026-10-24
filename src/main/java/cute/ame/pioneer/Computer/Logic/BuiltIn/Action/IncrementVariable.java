package cute.ame.pioneer.Computer.Logic.BuiltIn.Action;

import com.google.gson.annotations.SerializedName;
import cute.ame.pioneer.Computer.BlockEntity.MagneticCoreMemoryEntity;
import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Computer.Logic.Variable;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class IncrementVariable extends VariableAction {

    public final static ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "increment_variable");

    private Number value;

    public IncrementVariable(String variableName, Number value) {
        super(TYPE, variableName);
        this.value = value;
    }

    @Override
    public boolean evaluate(MagneticCoreMemoryEntity memory) {
        Object variableValue = variable.getValue();
        if (variableValue instanceof Number a) {
            Number result = a.doubleValue() + value.doubleValue();
            variable.assign(result);
            memory.setChanged();
        }
        return true;
    }

}
