package dev.xpolion.xpotriad;

import org.bukkit.Material;

import java.util.List;

public abstract class Fragment {

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final boolean glint;

    protected Fragment(
            String id,
            String displayName,
            Material material
    ) {
        this(
                id,
                displayName,
                material,
                List.of(),
                false
        );
    }

    protected Fragment(
            String id,
            String displayName,
            Material material,
            List<String> lore
    ) {
        this(
                id,
                displayName,
                material,
                lore,
                false
        );
    }

    protected Fragment(
            String id,
            String displayName,
            Material material,
            List<String> lore,
            boolean glint
    ) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.lore = List.copyOf(lore);
        this.glint = glint;
    }

    public final String getId() {
        return id;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final Material getMaterial() {
        return material;
    }

    public final List<String> getLore() {
        return lore;
    }

    public final boolean hasGlint() {
        return glint;
    }

    public abstract void execute(AbilityContext context);
}