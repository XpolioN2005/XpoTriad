package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.SmokeBombEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Creates a temporary smoke cloud around the caster (visual cover only).
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = +6 ticks
 * target           = SELF
 * effect           = SmokeBombState runtime (100 ticks, radius 1.0 -> 2.5)
 */
public final class SmokeBombFragment extends Fragment {

    public SmokeBombFragment() {
        super(
                "smoke_bomb",
                "Smoke Bomb Fragment",
                List.of(Component.text("Creates a temporary cloud of smoke.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("smoke_bomb", 10L),
                Rarity.COMMON,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("smoke_bomb", 6L),
                new SmokeBombEffect()
        );
    }
}