package dev.xpolion.xpotriad;

import org.bukkit.entity.Player;

public final class AbilityContext {

    private final Player player;
    private final Ability ability;

    public AbilityContext(Player player, Ability ability) {
        this.player = player;
        this.ability = ability;
    }

    public Player getPlayer() {
        return player;
    }

    public Ability getAbility() {
        return ability;
    }
}