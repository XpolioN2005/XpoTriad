package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.RewindState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Snapshots the caster's position and health once at activation and
 * restores them after a configured number of ticks.
 *
 * Targeting: SELF. Runtime required for recording and the delayed trigger.
 * Visual: exactly 2 blue rings rotate in opposite directions around the
 * caster while recording (first 10 ticks), then bursts on rewind.
 */
public final class RewindEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        int triggerTicks = cfg.effectInt("rewind", "trigger-ticks", 100);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        RewindState state = new RewindState(
                source,
                plugin,
                plugin.getParticleSystem(),
                triggerTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}