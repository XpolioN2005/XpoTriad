package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.ShieldState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reduces incoming damage by 40% for 100 ticks.
 *
 * Targeting: SELF. Runtime required (intercepts damage while active).
 * Visual: rotating particle ring at waist height (r0.7) for the full
 * 100 ticks, stopped immediately when the shield ends.
 */
public final class ShieldEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        ShieldState state = new ShieldState(
                source,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}