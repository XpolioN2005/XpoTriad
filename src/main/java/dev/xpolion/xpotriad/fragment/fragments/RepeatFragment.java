package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.RepeatEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Re-executes the previous applicable fragment one additional time.
 * Repeat may never execute another Repeat (chain depth 1).
 *
 * executionTime    = 5 ticks
 * rarity           = LEGENDARY
 * type             = MELEE
 * cooldownModifier = +15 ticks
 * target           = NONE (inherits the repeated fragment's targeting)
 * effect           = RepeatEffect (no independent runtime)
 */
public final class RepeatFragment extends Fragment {

    public RepeatFragment() {
        super(
                "repeat",
                "Repeat Fragment",
                List.of(Component.text("Repeats the previous fragment.", NamedTextColor.GRAY)),
                5L,
                Rarity.LEGENDARY,
                Type.MELEE,
                15L,
                new RepeatEffect()
        );
    }
}