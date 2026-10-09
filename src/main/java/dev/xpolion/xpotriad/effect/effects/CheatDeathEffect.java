package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.CheatDeathState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Grants complete incoming damage immunity for a configured duration.
 *
 * Targeting: SELF. Runtime required because damage must be intercepted
 * during the active window; ends automatically after the duration.
 * Visual: rotating golden particle sphere (r0.8) around the body for
 * the duration, stopped when immunity ends.
 */
public final class CheatDeathEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        int durationTicks = cfg.effectInt("cheat_death", "duration-ticks", 40);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        CheatDeathState state = new CheatDeathState(
                source,
                plugin,
                plugin.getParticleSystem(),
                durationTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}