package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.DelayEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Holds the engine for its execution time before the next stage.
 * The engine's native executionTime(20) + stage buffer hold IS the delay.
 *
 * executionTime    = 20 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = 0 ticks
 * target           = NONE
 * effect           = no-op (no runtime; engine hold only)
 */
public final class DelayFragment extends Fragment {

    public DelayFragment() {
        super(
                "delay",
                "Delay Fragment",
                List.of(Component.text("Delays the next stage.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("delay", 20L),
                Rarity.COMMON,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("delay", 0L),
                new DelayEffect()
        );
    }
}