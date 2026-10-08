package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.LifeStealEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Heals the caster for 25% of damage dealt for 100 ticks.
 *
 * executionTime    = 8 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = +10 ticks
 * target           = SINGLE_ENTITY
 * effect           = LifeStealState runtime (tracks the temporary effect)
 */
public final class LifeStealFragment extends Fragment {

    public LifeStealFragment() {
        super(
                "life_steal",
                "Life Steal Fragment",
                List.of(Component.text("Converts part of your damage into health.", NamedTextColor.GRAY)),
                8L,
                Rarity.EPIC,
                Type.MELEE,
                10L,
                new LifeStealEffect()
        );
    }
}