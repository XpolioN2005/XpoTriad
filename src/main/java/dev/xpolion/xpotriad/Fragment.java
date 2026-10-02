package dev.xpolion.xpotriad;

public abstract class Fragment {

    private final String id;
    private final String name;

    protected Fragment(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public abstract void execute(AbilityContext context);
}