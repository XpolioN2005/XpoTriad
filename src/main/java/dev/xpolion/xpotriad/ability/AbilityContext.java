package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.targeting.Targeting;
import org.bukkit.entity.Player;

/**
 * Represents one active execution of an Ability.
 * Passed through all three stages: PRE_CAST → CAST → POST_CAST.
 */
public final class AbilityContext {

    private final Player source;
    private final Ability ability;
    private final Targeting targeting;

    public AbilityContext(Player source, Ability ability, Targeting targeting) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (ability == null) {
            throw new IllegalArgumentException("Ability cannot be null");
        }
        if (targeting == null) {
            throw new IllegalArgumentException("Targeting cannot be null");
        }
        this.source = source;
        this.ability = ability;
        this.targeting = targeting;
    }

    public Player getSource() {
        return source;
    }

    public Ability getAbility() {
        return ability;
    }

    public Targeting getTargeting() {
        return targeting;
    }
}
