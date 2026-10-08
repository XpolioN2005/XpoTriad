package dev.xpolion.xpotriad.fragment.fragments;

import java.util.List;

import dev.xpolion.xpotriad.effect.effects.ShieldEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Temporarily reduces incoming damage by 40%.
 *
 * executionTime    = 10 ticks
 * rarity           = COMMON
 * type             = MELEE
 * cooldownModifier = +6 ticks
 * target           = SELF
 * effect           = ShieldState runtime (100 ticks, rotating r0.7 ring)
 */
public final class ShieldFragment extends Fragment {

    public ShieldFragment() {
        super(
                "shield",
                "Shield Fragment",
                List.of(Component.text("Temporarily reduces incoming damage.", NamedTextColor.GRAY)),
                10L,
                Rarity.COMMON,
                Type.MELEE,
                6L,
                new ShieldEffect()
        );
    }
}