package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.LifeStealState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Heals the caster for a configured ratio of qualifying damage dealt.
 *
 * Targeting: SINGLE_ENTITY context (the damage source is the caster).
 * Cannot exceed maximum health (setHealth is clamped).
 * Runtime required to track the temporary effect; ends after the configured duration.
 */
public final class LifeStealEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        int durationTicks = cfg.effectInt("life_steal", "duration-ticks", 100);
        double healRatio = cfg.effectDouble("life_steal", "heal-ratio", 0.25);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        LifeStealState state = new LifeStealState(
                source,
                plugin,
                plugin.getParticleSystem(),
                durationTicks,
                healRatio
        );

        plugin.getRuntimeManager().start(state);
    }
}