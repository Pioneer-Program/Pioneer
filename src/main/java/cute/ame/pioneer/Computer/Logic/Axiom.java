package cute.ame.pioneer.Computer.Logic;

public abstract class Axiom {

    private final String modId;
    private final String name;

    public Axiom(String type) {
        String[] split = type.split(":");
        if (split.length != 2)
            throw new IllegalArgumentException("Invalid Axiom type: " + type);
        this.modId = split[0];
        this.name = split[1];
    }

    public Axiom(String modId, String name) {
        this.modId = modId;
        this.name = name;
    }

    public String getType() {
        return this.modId +  ":" + this.name;
    }

    public String getModId() {
        return this.modId;
    }

    public String getName() {
        return this.name;
    }


}
