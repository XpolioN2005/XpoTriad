package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.effect.effects.SpeedEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Grants the source player a speed boost.
 *
 * executionTime    = 20 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = 0 ticks (no change to base cooldown)
 */
public final class SpeedFragment extends Fragment {

    public SpeedFragment() {
        super(
                "speed",
                "Speed Fragment",
                List.of(Component.text("Grants increased movement speed.", NamedTextColor.GRAY)),
                20L,
                Rarity.COMMON,
                Type.MELEE,
                0L,
                new SpeedEffect()
        );
    }
}