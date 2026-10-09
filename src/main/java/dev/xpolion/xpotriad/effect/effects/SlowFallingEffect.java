package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies Slow Falling.
 *
 * Targeting: SELF (caster). No custom runtime required —
 * vanilla slow-falling particles come from the potion effect itself.
 */
public final class SlowFallingEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        BalanceConfig cfg = BalanceConfig.get();
        int duration = cfg.effectInt("slow_falling", "potion-duration-ticks", 100);
        int amplifier = cfg.effectInt("slow_falling", "amplifier", 0);

        Player source = context.getSource();

        source.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                duration,
                amplifier,
                false,
                true,
                true
        ));
    }
}