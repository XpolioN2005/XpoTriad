package dev.xpolion.xpotriad.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class SpeedEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        context.getSource().addPotionEffect(
            new PotionEffect(
                PotionEffectType.SPEED,
                100,
                1,
                false,
                false,
                false
            )
        );
    }
}