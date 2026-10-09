package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.MarkEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;

/**
 * Applies a temporary 10-second Mark to a target entity.
 * Marked targets take 1.5x damage on the next incoming damage instance.
 *
 * executionTime    = 10 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = +30 ticks
 */
public final class MarkFragment extends Fragment {

    public MarkFragment() {
        super(
                "mark",
                "Mark Fragment",
                List.of(
                        Component.text("Marks a target for 10 seconds."),
                        Component.text("The next damage dealt to the target is multiplied by 1.5x.")
                ),
                BalanceConfig.get().executionTime("mark", 10L),
                Rarity.EPIC,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("mark", 30L),
                new MarkEffect()
        );
    }
}
