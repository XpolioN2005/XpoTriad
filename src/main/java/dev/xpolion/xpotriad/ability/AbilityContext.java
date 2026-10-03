package dev.xpolion.xpotriad.ability;

import org.bukkit.entity.Player;

/**
 * Represents one active execution of an Ability.
 * Minimal context containing only the source player and ability definition.
 */
public final class AbilityContext {

    private final Player source;
    private final Ability ability;

    public AbilityContext(Player source, Ability ability) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (ability == null) {
            throw new IllegalArgumentException("Ability cannot be null");
        }
        this.source = source;
        this.ability = ability;
    }

    public Player getSource() {
        return source;
    }

    public Ability getAbility() {
        return ability;
    }
}
