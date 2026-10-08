package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.HoundCallEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Summons 5 tamed wolves owned by the caster for 200 ticks.
 *
 * executionTime    = 15 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = +15 ticks
 * target           = SELF
 * effect           = HoundCallState runtime (tracks lifetime + cleanup)
 */
public final class HoundCallFragment extends Fragment {

    public HoundCallFragment() {
        super(
                "hound_call",
                "Hound Call Fragment",
                List.of(Component.text("Summons spectral hunting hounds.", NamedTextColor.GRAY)),
                15L,
                Rarity.EPIC,
                Type.MELEE,
                15L,
                new HoundCallEffect()
        );
    }
}