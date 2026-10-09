package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.SlowFallingEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Applies Slow Falling for 100 ticks (no custom runtime).
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = RANGED
 * cooldownModifier = +3 ticks
 * target           = SELF
 * effect           = Slow Falling 100 ticks, amplifier 0
 */
public final class SlowFallingFragment extends Fragment {

    public SlowFallingFragment() {
        super(
                "slow_falling",
                "Slow Falling Fragment",
                List.of(Component.text("Slows the target's fall.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("slow_falling", 10L),
                Rarity.COMMON,
                Type.RANGED,
                BalanceConfig.get().cooldownModifier("slow_falling", 3L),
                new SlowFallingEffect()
        );
    }
}