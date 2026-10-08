package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.CheatDeathState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Grants complete incoming damage immunity for 40 ticks.
 *
 * Targeting: SELF. Runtime required because damage must be intercepted
 * during the active window; ends automatically after 40 ticks.
 * Visual: rotating golden particle sphere (r0.8) around the body for
 * exactly 40 ticks, stopped when immunity ends.
 */
public final class CheatDeathEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        CheatDeathState state = new CheatDeathState(
                source,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}