package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.LeapEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Launches the caster forward and upward.
 *
 * executionTime    = 8 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = +3 ticks
 * target           = SELF
 * effect           = forward 1.0 / vertical 0.8 launch (no gameplay runtime;
 *                   trail visual is a finite 6-tick particle)
 */
public final class LeapFragment extends Fragment {

    public LeapFragment() {
        super(
                "leap",
                "Leap Fragment",
                List.of(Component.text("Launches you forward and upward.", NamedTextColor.GRAY)),
                8L,
                Rarity.COMMON,
                Type.MELEE,
                3L,
                new LeapEffect()
        );
    }
}