package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;

import org.bukkit.entity.Player;

/**
 * Represents one active execution of an Ability.
 * Minimal context containing only the source player and ability definition.
 */
public final class AbilityContext {

    private final Player source;
    private final Ability ability;

    /**
     * Last fragment that finished executing in this activation.
     * Written by AbilityEngine after each Fragment.execute() call.
     * Used by Repeat to re-execute the previous applicable fragment.
     * Null until the first fragment of this activation has executed.
     */
    private Fragment lastExecutedFragment;

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

    public Fragment getLastExecutedFragment() {
        return lastExecutedFragment;
    }

    public void setLastExecutedFragment(Fragment lastExecutedFragment) {
        this.lastExecutedFragment = lastExecutedFragment;
    }
}
