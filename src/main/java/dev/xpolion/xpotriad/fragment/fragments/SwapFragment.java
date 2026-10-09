package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.SwapEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Immediately swaps the caster and target positions (both validated, no runtime).
 *
 * executionTime    = 8 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = +14 ticks
 * target           = SINGLE_ENTITY
 */
public final class SwapFragment extends Fragment {

    public SwapFragment() {
        super(
                "swap",
                "Swap Fragment",
                List.of(Component.text("Swaps your position with the target.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("swap", 8L),
                Rarity.EPIC,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("swap", 14L),
                new SwapEffect()
        );
    }
}