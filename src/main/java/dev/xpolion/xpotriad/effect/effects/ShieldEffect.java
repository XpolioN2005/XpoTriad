package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.ShieldState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reduces incoming damage for a configured duration.
 *
 * Targeting: SELF. Runtime required (intercepts damage while active).
 * Visual: rotating particle ring at waist height (r0.7) for the full
 * duration, stopped immediately when the shield ends.
 */
public final class ShieldEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        int durationTicks = cfg.effectInt("shield", "duration-ticks", 100);
        double damageMultiplier = cfg.effectDouble("shield", "damage-multiplier", 0.6);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        ShieldState state = new ShieldState(
                source,
                plugin,
                plugin.getParticleSystem(),
                durationTicks,
                damageMultiplier
        );

        plugin.getRuntimeManager().start(state);
    }
}