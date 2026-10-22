package cute.ame.pioneer.Computer.Logic;

public class Function extends Axiom {

    private String description;
    private Action action;
    private Conditional condition;
    private int interval;
    private int tickNumber;


    public Function(String name, String description, int interval, Action action, Conditional condition) {
        super(name);
        this.description = description;
        this.interval = interval;
        this.action = action;
        this.condition = condition;
        this.tickNumber = 0;
    }

    public void tick() {
        ++tickNumber;
        if (condition == null || condition.isSatisfied())
            action.execute();
    }

    public String getDescription() {
        return description;
    }

    public int getInterval() {
        return interval;
    }

    public Action getAction() {
        return action;
    }
    public Conditional getCondition() {
        return condition;
    }
}


