package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.GravityPullEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Pulls nearby enemies toward a selected point.
 *
 * executionTime    = 15 ticks
 * rarity           = RARE
 * type             = MELEE
 * cooldownModifier = +12 ticks
 * target           = POINT
 * effect           = GravityPullState runtime (r4, 60 ticks, pull every 5 ticks)
 */
public final class GravityPullFragment extends Fragment {

    public GravityPullFragment() {
        super(
                "gravity_pull",
                "Gravity Pull Fragment",
                List.of(Component.text("Pulls nearby enemies toward a point.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("gravity_pull", 15L),
                Rarity.RARE,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("gravity_pull", 12L),
                new GravityPullEffect()
        );
    }
}