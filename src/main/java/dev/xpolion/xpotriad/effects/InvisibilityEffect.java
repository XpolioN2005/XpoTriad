package dev.xpolion.xpotriad.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class InvisibilityEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        context.getSource().addPotionEffect(
            new PotionEffect(
                PotionEffectType.INVISIBILITY,
                100,
                0,
                false,
                false,
                false
            )
        );
    }
}