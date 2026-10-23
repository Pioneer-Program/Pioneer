package cute.ame.pioneer.Computer.Logic.BuiltIn.Condition;

import cute.ame.pioneer.Computer.Logic.Condition;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

public class OrCondition extends Condition {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "or");

    private Condition[] conditions;

    public OrCondition(Condition[] conditions) {
        super(TYPE);
        this.conditions = conditions;
    }

    @Override
    public boolean isSatisfied() {
        if (conditions == null)
            return false;
        for  (Condition condition : conditions) {
            if (condition.isSatisfied())
                return true;
        }
        return false;
    }

    public  Condition[] getConditions() {
        return conditions;
    }

    public void setConditions(Condition[] conditions) {
        this.conditions = conditions;
    }
}
