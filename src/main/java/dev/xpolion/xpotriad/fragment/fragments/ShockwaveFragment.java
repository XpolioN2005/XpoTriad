package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.effects.ShockwaveEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Slams the caster down to the ground and erupts a fall-scaled shockwave that
 * knocks back and damages nearby enemies.
 *
 * executionTime    = 5 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = 15 ticks
 */
public final class ShockwaveFragment extends Fragment {

    public ShockwaveFragment() {
        super(
                "shockwave",
                "Shockwave Fragment",
                List.of(
                        Component.text("Slams you to the ground and erupts.", NamedTextColor.GRAY),
                        Component.text("Radius scales with fall distance.", NamedTextColor.GRAY)
                ),
                BalanceConfig.get().executionTime("shockwave", 5L),
                Rarity.EPIC,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("shockwave", 15L),
                new ShockwaveEffect()
        );
    }
}
