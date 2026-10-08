package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.CleanseEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Removes all negative potion effects from the target (immediate, no runtime).
 *
 * executionTime    = 8 ticks
 * rarity           = UNCOMMON
 * type             = MELEE
 * cooldownModifier = +5 ticks
 * target           = SINGLE_ENTITY
 */
public final class CleanseFragment extends Fragment {

    public CleanseFragment() {
        super(
                "cleanse",
                "Cleanse Fragment",
                List.of(Component.text("Removes negative effects from the target.", NamedTextColor.GRAY)),
                8L,
                Rarity.UNCOMMON,
                Type.MELEE,
                5L,
                new CleanseEffect()
        );
    }
}