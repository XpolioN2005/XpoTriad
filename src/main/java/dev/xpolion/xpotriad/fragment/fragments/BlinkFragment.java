package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.BlinkEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Teleports the caster to a validated location a short distance ahead.
 *
 * executionTime    = 5 ticks
 * rarity           = RARE
 * type             = MELEE
 * cooldownModifier = +10 ticks
 * target           = POINT
 * effect           = immediate teleport (max 8 blocks, no gameplay runtime)
 */
public final class BlinkFragment extends Fragment {

    public BlinkFragment() {
        super(
                "blink",
                "Blink Fragment",
                List.of(Component.text("Teleports you a short distance.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("blink", 5L),
                Rarity.RARE,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("blink", 10L),
                new BlinkEffect()
        );
    }
}