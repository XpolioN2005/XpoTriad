package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.KnockbackEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Pushes the target directly away from the caster.
 *
 * executionTime    = 5 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = +2 ticks
 * target           = SINGLE_ENTITY
 * effect           = horizontal 1.5 / vertical 0.35 knockback (no runtime)
 */
public final class KnockbackFragment extends Fragment {

    public KnockbackFragment() {
        super(
                "knockback",
                "Knockback Fragment",
                List.of(Component.text("Blasts the target backward.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("knockback", 5L),
                Rarity.COMMON,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("knockback", 2L),
                new KnockbackEffect()
        );
    }
}