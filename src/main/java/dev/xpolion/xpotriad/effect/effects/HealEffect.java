package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class HealEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        context.getSource().addPotionEffect(
            new PotionEffect(
                PotionEffectType.REGENERATION,
                100,
                0,
                false,
                false,
                false
            )
        );
    }
}