package cute.ame.pioneer.Computer.Logic;

import cute.ame.pioneer.Computer.Logic.BuiltIn.Action.ChangeVariable;
import cute.ame.pioneer.Computer.Logic.BuiltIn.Condition.OrCondition;
import cute.ame.pioneer.Computer.Logic.BuiltIn.Function.ActionOverTime;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AxiomFactory {

    private static final HashMap<ResourceLocation, Class<? extends Axiom>> AXIOMS = new  HashMap<>(Map.ofEntries(
            Map.entry(OrCondition.TYPE, OrCondition.class),
            Map.entry(ActionOverTime.TYPE, ActionOverTime.class),
            Map.entry(ChangeVariable.TYPE, ChangeVariable.class)
    ));

    public static Class<? extends Axiom> getAxiomClass(ResourceLocation type) {
        Class<? extends Axiom> axiomClass = AXIOMS.get(type);
        if (axiomClass == null)
            throw new IllegalArgumentException("No axiom class found with type " + type);
        return axiomClass;
    }

    public static void addAxiomClass(ResourceLocation type, Class<? extends Axiom> axiomClass) {
        AXIOMS.putIfAbsent(type, axiomClass);
    }

}
