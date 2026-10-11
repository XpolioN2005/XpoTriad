package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.effects.BlindEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Blinds nearby enemies with the Blindness effect.
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = 0 ticks
 */
public final class BlindFragment extends Fragment {

    public BlindFragment() {
        super(
                "blind",
                "Blind Fragment",
                List.of(
                        Component.text("Blinds nearby enemies.", NamedTextColor.GRAY),
                        Component.text("Applies Blindness in a short radius.", NamedTextColor.GRAY)
                ),
                BalanceConfig.get().executionTime("blind", 10L),
                Rarity.COMMON,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("blind", 0L),
                new BlindEffect()
        );
    }
}
