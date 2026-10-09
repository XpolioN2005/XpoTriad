package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.MassFreezeEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Freezes every entity in a radius around the caster's position at cast
 * time for the configured duration (velocity zeroed each tick — works on
 * players too). Center and frost ring are static, never follow the caster.
 *
 * executionTime    = 15 ticks
 * rarity           = LEGENDARY
 * type             = RANGED
 * cooldownModifier = +18 ticks
 * target           = SELF (static center fixed at the cast position)
 * effect           = MassFreezeState runtime (ends early for invalid entities)
 */
public final class MassFreezeFragment extends Fragment {

    public MassFreezeFragment() {
        super(
                "mass_freeze",
                "Mass Freeze Fragment",
                List.of(Component.text("Freezes enemies in an area.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("mass_freeze", 15L),
                Rarity.LEGENDARY,
                Type.RANGED,
                BalanceConfig.get().cooldownModifier("mass_freeze", 18L),
                new MassFreezeEffect()
        );
    }
}