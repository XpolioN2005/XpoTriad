package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.ThornsState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reflects 30% of incoming melee damage back to the attacker for 100 ticks.
 *
 * Targeting: SELF. Runtime required — waits for incoming attacks.
 * Reflected damage never recursively triggers Thorns (shared reentrancy
 * guard inside ThornsState).
 */
public final class ThornsEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        ThornsState state = new ThornsState(
                source,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}