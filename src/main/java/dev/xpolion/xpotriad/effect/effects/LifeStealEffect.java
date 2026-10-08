package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.LifeStealState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Heals the caster for 25% of qualifying damage dealt for 100 ticks.
 *
 * Targeting: SINGLE_ENTITY context (the damage source is the caster).
 * Cannot exceed maximum health (setHealth is clamped).
 * Runtime required to track the temporary effect; ends after 100 ticks.
 */
public final class LifeStealEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        LifeStealState state = new LifeStealState(
                source,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}