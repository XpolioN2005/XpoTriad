package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.LevitationEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Applies Levitation for 60 ticks, amplifier 0 (no custom runtime).
 *
 * executionTime    = 10 ticks
 * rarity           = UNCOMMON
 * type             = RANGED
 * cooldownModifier = +6 ticks
 * target           = SINGLE_ENTITY
 */
public final class LevitationFragment extends Fragment {

    public LevitationFragment() {
        super(
                "levitation",
                "Levitation Fragment",
                List.of(Component.text("Lifts the target into the air.", NamedTextColor.GRAY)),
                10L,
                Rarity.UNCOMMON,
                Type.RANGED,
                6L,
                new LevitationEffect()
        );
    }
}