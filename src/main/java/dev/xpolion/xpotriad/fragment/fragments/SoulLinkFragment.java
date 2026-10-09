package dev.xpolion.xpotriad.fragment.fragments;

import dev.xpolion.xpotriad.config.BalanceConfig;
import java.util.List;

import dev.xpolion.xpotriad.effect.effects.SoulLinkEffect;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.Fragment.Rarity;
import dev.xpolion.xpotriad.fragment.Fragment.Type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Links caster and target, transferring 30% of damage between them
 * for 100 ticks.
 *
 * executionTime    = 12 ticks
 * rarity           = EPIC
 * type             = MELEE
 * cooldownModifier = +15 ticks
 * target           = SINGLE_ENTITY
 * effect           = SoulLinkState runtime (ends early if either is invalid)
 */
public final class SoulLinkFragment extends Fragment {

    public SoulLinkFragment() {
        super(
                "soul_link",
                "Soul Link Fragment",
                List.of(Component.text("Links you to the target and shares damage.", NamedTextColor.GRAY)),
                BalanceConfig.get().executionTime("soul_link", 12L),
                Rarity.EPIC,
                Type.MELEE,
                BalanceConfig.get().cooldownModifier("soul_link", 15L),
                new SoulLinkEffect()
        );
    }
}