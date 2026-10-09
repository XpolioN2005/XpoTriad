package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.effects.HealEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Grants the source player regeneration.
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = 0 ticks (no change to base cooldown)
 */
public final class HealFragment extends Fragment {

    public HealFragment() {
        super(
                "heal",
                "Heal Fragment",
                List.of(Component.text("Grants regeneration.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("heal", 10L),
                Rarity.COMMON,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("heal", 0L),
                new HealEffect()
        );
    }
}