package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.effects.AreaPoisonEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Poisons all enemies in a targeted area, leaving a green smoke cloud.
 *
 * executionTime    = 10 ticks
 * rarity           = UNCOMMON
 * type             = RANGED (point-targeted)
 * cooldownModifier = 8 ticks
 */
public final class AreaPoisonFragment extends Fragment {

    public AreaPoisonFragment() {
        super(
                "area_poison",
                "Area Poison Fragment",
                List.of(
                        Component.text("Poisons enemies in a targeted area.", NamedTextColor.GRAY),
                        Component.text("Leaves a lingering green cloud.", NamedTextColor.GRAY)
                ),
                BalanceConfig.get().executionTime("area_poison", 10L),
                Rarity.UNCOMMON,
                Type.RANGED,
                BalanceConfig.get().cooldownModifier("area_poison", 8L),
                new AreaPoisonEffect()
        );
    }
}
