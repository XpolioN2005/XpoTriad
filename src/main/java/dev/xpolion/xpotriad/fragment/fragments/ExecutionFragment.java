package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.ExecutionEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Deals 10 bonus damage to targets at or below 25% health (instant, no runtime).
 *
 * executionTime    = 5 ticks
 * rarity           = RARE
 * type             = MELEE
 * cooldownModifier = +8 ticks
 * target           = SINGLE_ENTITY
 */
public final class ExecutionFragment extends Fragment {

    public ExecutionFragment() {
        super(
                "execution",
                "Execution Fragment",
                List.of(Component.text("Deals bonus damage to weakened targets.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("execution", 5L),
                Rarity.RARE,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("execution", 8L),
                new ExecutionEffect()
        );
    }
}