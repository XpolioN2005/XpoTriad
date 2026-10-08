package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.effect.effects.InvisibilityEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Grants the source player invisibility.
 *
 * executionTime    = 10 ticks
 * rarity           = UNCOMMON
 * type             = MELEE
 * cooldownModifier = +10 ticks
 */
public final class InvisibilityFragment extends Fragment {

    public InvisibilityFragment() {
        super(
                "invisibility",
                "Invisibility Fragment",
                List.of(Component.text("Grants invisibility.", NamedTextColor.GRAY)),
                10L,
                Rarity.UNCOMMON,
                Type.MELEE,
                10L,
                new InvisibilityEffect()
        );
    }
}