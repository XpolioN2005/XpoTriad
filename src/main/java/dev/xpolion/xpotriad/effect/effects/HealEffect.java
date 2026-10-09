package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class HealEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        BalanceConfig cfg = BalanceConfig.get();
        int duration = cfg.effectInt("heal", "potion-duration-ticks", 100);
        int amplifier = cfg.effectInt("heal", "amplifier", 0);

        context.getSource().addPotionEffect(
            new PotionEffect(
                PotionEffectType.REGENERATION,
                duration,
                amplifier,
                false,
                false,
                false
            )
        );
    }
}