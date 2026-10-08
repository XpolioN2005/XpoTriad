package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.MassFreezeEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Freezes every entity in a 5-block area for 60 ticks (velocity zeroed
 * each tick — works on players too).
 *
 * executionTime    = 15 ticks
 * rarity           = LEGENDARY
 * type             = RANGED
 * cooldownModifier = +18 ticks
 * target           = AREA
 * effect           = MassFreezeState runtime (60 ticks, ends early for invalid entities)
 */
public final class MassFreezeFragment extends Fragment {

    public MassFreezeFragment() {
        super(
                "mass_freeze",
                "Mass Freeze Fragment",
                List.of(Component.text("Freezes enemies in an area.", NamedTextColor.GRAY)),
                15L,
                Rarity.LEGENDARY,
                Type.RANGED,
                18L,
                new MassFreezeEffect()
        );
    }
}