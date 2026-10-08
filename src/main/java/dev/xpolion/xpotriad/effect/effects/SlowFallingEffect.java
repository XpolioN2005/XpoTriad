package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies Slow Falling for 100 ticks.
 *
 * Targeting: SELF (caster). No custom runtime required —
 * vanilla slow-falling particles come from the potion effect itself.
 */
public final class SlowFallingEffect implements Effect {

    private static final int DURATION_TICKS = 100;

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        source.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                DURATION_TICKS,
                0,
                false,
                true,
                true
        ));
    }
}