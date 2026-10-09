package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SpeedEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        BalanceConfig cfg = BalanceConfig.get();
        int duration = cfg.effectInt("speed", "potion-duration-ticks", 100);
        int amplifier = cfg.effectInt("speed", "amplifier", 1);

        context.getSource().addPotionEffect(
            new PotionEffect(
                PotionEffectType.SPEED,
                duration,
                amplifier,
                false,
                false,
                false
            )
        );
    }
}