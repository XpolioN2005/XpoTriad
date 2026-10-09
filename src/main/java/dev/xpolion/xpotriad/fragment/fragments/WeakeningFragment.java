package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.WeaknessEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Applies Weakness to the target.
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = +3 ticks
 * target           = SINGLE_ENTITY
 * effect           = Weakness 100 ticks, amplifier 0 (no runtime)
 */
public final class WeakeningFragment extends Fragment {

    public WeakeningFragment() {
        super(
                "weakening",
                "Weakening Fragment",
                List.of(Component.text("Weakens the target.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("weakening", 10L),
                Rarity.COMMON,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("weakening", 3L),
                new WeaknessEffect()
        );
    }
}