package dev.xpolion.xpotriad.fragment;

import dev.xpolion.xpotriad.effects.MarkEffect;

import java.util.List;

/**
 * Applies a temporary 10-second Mark to a target entity.
 * Marked targets take 1.5x damage on the next incoming damage instance.
 *
 * executionTime    = 10 ticks
 * rarity           = EPIC
 * cooldownModifier = +30 ticks
 */
public final class MarkFragment extends Fragment {

    public MarkFragment() {
        super(
                "mark",
                "Mark Fragment",
                List.of(
                        "Marks a target for 10 seconds.",
                        "The next damage dealt to the target is multiplied by 1.5x."
                ),
                10L,
                Rarity.EPIC,
                30L,
                new MarkEffect()
        );
    }
}
