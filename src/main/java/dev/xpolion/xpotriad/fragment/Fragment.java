package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.ability.AbilityContext;
import org.bukkit.Material;

import java.util.List;

/**
 * Immutable definition of a gameplay unit that executes one Effect per stage.
 * Fragment is weapon-agnostic; it does not know about targeting or weapon types.
 *
 * executionTime defines the minimum window for this fragment's effect.
 * The engine adds a 5-tick buffer on top.
 */
public abstract class Fragment {

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;
    private final boolean glint;
    private final long executionTime;

    protected Fragment(
            String id,
            String displayName,
            Material material
    ) {
        this(id, displayName, material, List.of(), false, 0L);
    }

    protected Fragment(
            String id,
            String displayName,
            Material material,
            List<String> lore
    ) {
        this(id, displayName, material, lore, false, 0L);
    }

    protected Fragment(
            String id,
            String displayName,
            Material material,
            List<String> lore,
            boolean glint
    ) {
        this(id, displayName, material, lore, glint, 0L);
    }

    protected Fragment(
            String id,
            String displayName,
            Material material,
            List<String> lore,
            boolean glint,
            long executionTime
    ) {
        if (executionTime < 0) {
            throw new IllegalArgumentException("executionTime cannot be negative");
        }
        this.id            = id;
        this.displayName   = displayName;
        this.material      = material;
        this.lore          = List.copyOf(lore);
        this.glint         = glint;
        this.executionTime = executionTime;
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

    /**
     * The minimum execution window for this fragment's effect, in ticks.
     * The engine waits executionTime + 5 ticks before advancing to the next stage.
     */
    public final long getExecutionTime() {
        return executionTime;
    }

    public abstract void execute(AbilityContext context);
}
