package cute.ame.pioneer.Computer.Logic.BuiltIn.Function;


import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Computer.Logic.Condition;
import cute.ame.pioneer.Computer.Logic.Function;
import cute.ame.pioneer.Computer.Logic.Variable;
import cute.ame.pioneer.Pioneer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;

public class ActionOverTime extends Function {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Pioneer.MODID, "action_over_time");

    private int interval;
    private transient int tick;

    public ActionOverTime(Action action, Condition condition, int interval) {
        super(TYPE, action, condition);
        this.interval = interval;
        this.tick = 0;
    }

    @Override
    public boolean execute(HashMap<String, Variable<?>> variables) {
        if (tick >= interval) {
            tick = 0;
            return super.execute(variables);
        }
        ++tick;
        return false;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
